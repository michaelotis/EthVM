package io.enkrypt.kafka.streams.processors

import io.enkrypt.avro.capture.CanonicalKeyRecord
import io.enkrypt.avro.common.TraceLocationRecord
import io.enkrypt.avro.processing.FungibleBalanceDeltaListRecord
import io.enkrypt.avro.processing.FungibleBalanceDeltaRecord
import io.enkrypt.avro.processing.FungibleBalanceDeltaType
import io.enkrypt.avro.processing.FungibleBalanceKeyRecord
import io.enkrypt.avro.processing.FungibleBalanceRecord
import io.enkrypt.avro.processing.FungibleTokenType
import io.enkrypt.common.extensions.getAmountBI
import io.enkrypt.common.extensions.getNumberBI
import io.enkrypt.common.extensions.getTransactionFeeBI
import io.enkrypt.common.extensions.reverse
import io.enkrypt.common.extensions.setAmountBI
import io.enkrypt.common.extensions.setBlockNumberBI
import io.enkrypt.common.extensions.toEtherBalanceDeltas
import io.enkrypt.common.extensions.toFungibleBalanceDeltas
import io.enkrypt.kafka.streams.Serdes
import io.enkrypt.kafka.streams.config.Topics.CanonicalBlockAuthors
import io.enkrypt.kafka.streams.config.Topics.CanonicalBlocks
import io.enkrypt.kafka.streams.config.Topics.CanonicalReceiptErc20Deltas
import io.enkrypt.kafka.streams.config.Topics.CanonicalMinerFeesEtherDeltas
import io.enkrypt.kafka.streams.config.Topics.CanonicalReceipts
import io.enkrypt.kafka.streams.config.Topics.CanonicalTraces
import io.enkrypt.kafka.streams.config.Topics.CanonicalTracesEtherDeltas
import io.enkrypt.kafka.streams.config.Topics.CanonicalTransactionFees
import io.enkrypt.kafka.streams.config.Topics.CanonicalTransactionFeesEtherDeltas
import io.enkrypt.kafka.streams.config.Topics.FungibleBalanceDeltas
import io.enkrypt.kafka.streams.config.Topics.FungibleBalances
import io.enkrypt.kafka.streams.transformers.OncePerBlockTransformer
import io.enkrypt.kafka.streams.utils.ERC20Abi
import io.enkrypt.kafka.streams.utils.toTopic
import mu.KotlinLogging
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Grouped
import org.apache.kafka.streams.kstream.JoinWindows
import org.apache.kafka.streams.kstream.Joined
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.kstream.TransformerSupplier
import java.math.BigInteger
import java.time.Duration
import java.util.Properties
import org.apache.kafka.common.serialization.Serdes as KafkaSerdes

class FungibleBalanceProcessor : AbstractKafkaProcessor() {

  override val id: String = "fungible-balance-processor"

  override val kafkaProps: Properties = Properties()
    .apply {
      putAll(baseKafkaProps.toMap())
      put(StreamsConfig.APPLICATION_ID_CONFIG, id)
      put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 4)
      put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000L)
      put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 2000000000)
    }

  override val logger = KotlinLogging.logger {}

  override fun buildTopology(): Topology {

    val builder = StreamsBuilder().apply {
      addStateStore(OncePerBlockTransformer.canonicalRecordsStore(appConfig.unitTesting))
    }

    syntheticEtherDeltas(builder)

    etherDeltasForTraces(builder)
    etherDeltasForFees(builder)
    erc20DeltasForReceipts(builder)

    aggregateBalances(builder)

    // Generate the topology
    return builder.build()
  }

  private fun aggregateBalances(builder: StreamsBuilder) {

    FungibleBalanceDeltas.stream(builder)
      .groupByKey(Grouped.with(Serdes.FungibleBalanceKey(), Serdes.FungibleBalanceDelta()))
      .aggregate(
        {
          FungibleBalanceRecord.newBuilder()
            .setAmountBI(BigInteger.ZERO)
            .build()
        },
        { _, delta, balance ->

          FungibleBalanceRecord.newBuilder()
            .setAmountBI(delta.getAmountBI() + balance.getAmountBI())
            .build()
        },
        Materialized.with(Serdes.FungibleBalanceKey(), Serdes.FungibleBalance())
      )
      .toStream()
      .toTopic(FungibleBalances)

    FungibleBalances.stream(builder)
      .peek { k, v -> logger.info { "Balance update | ${k.getAddress()}, ${k.getContract()} -> ${v.getAmount()}" } }
  }

  /**
   * Premine and other synthetic transfers such as DAO hard fork
   */
  private fun syntheticEtherDeltas(builder: StreamsBuilder) {

    // add a transformer to guarantee we only emit once per block number so we don't re-introduce synthetic events in the event of a fork

    val canonicalBlocks = CanonicalBlocks.stream(builder)
      .transform(
        TransformerSupplier { OncePerBlockTransformer(appConfig.unitTesting) },
        *OncePerBlockTransformer.STORE_NAMES
      )

    // premine balances

    canonicalBlocks
      .flatMap { k, _ ->

        if (k.getNumberBI() > BigInteger.ZERO)
          emptyList()
        else {

          var deltas =
            netConfig.genesis
              .accounts
              .entries
              .map { (address, premine) ->

                val balance = premine.balance

                FungibleBalanceDeltaRecord.newBuilder()
                  .setTokenType(FungibleTokenType.ETHER)
                  .setDeltaType(FungibleBalanceDeltaType.PREMINE_BALANCE)
                  .setTraceLocation(
                    TraceLocationRecord.newBuilder()
                      .setBlockNumberBI(BigInteger.ZERO)
                      .build()
                  )
                  .setAddress(address)
                  .setAmount(balance)
                  .build()
              }

          // block reward

          deltas = deltas + FungibleBalanceDeltaRecord.newBuilder()
            .setTokenType(FungibleTokenType.ETHER)
            .setDeltaType(FungibleBalanceDeltaType.BLOCK_REWARD)
            .setTraceLocation(
              TraceLocationRecord.newBuilder()
                .setBlockNumberBI(BigInteger.ZERO)
                .build()
            )
            .setAddress("0x0000000000000000000000000000000000000000")
            .setAmountBI(
              netConfig.chainConfigForBlock(BigInteger.ZERO).constants.blockReward
            ).build()

          deltas.map { delta ->
            KeyValue(
              FungibleBalanceKeyRecord.newBuilder()
                .setAddress(delta.getAddress())
                .build(),
              FungibleBalanceDeltaRecord.newBuilder(delta)
                .setAddress(null)
                .build()
            )
          }
        }
      }.toTopic(FungibleBalanceDeltas)

    //

    canonicalBlocks
      .flatMap { k, _ ->

        val blockNumber = k.getNumberBI()

        netConfig
          .chainConfigForBlock(blockNumber)
          .hardForkFungibleDeltas(blockNumber)
          .map { delta ->

            KeyValue(
              FungibleBalanceKeyRecord.newBuilder()
                .setAddress(delta.getAddress())
                .build(),
              FungibleBalanceDeltaRecord.newBuilder(delta)
                .setAddress(null)
                .build()
            )
          }
      }.toTopic(FungibleBalanceDeltas)
  }

  /**
   *
   */
  private fun etherDeltasForTraces(builder: StreamsBuilder) {

    CanonicalTraces.stream(builder)
      .mapValues { _, tracesList ->

        val blockHash = tracesList.getTraces().firstOrNull()?.getBlockHash()

        when (tracesList) {
          null -> null
          else -> {

            FungibleBalanceDeltaListRecord.newBuilder()
              .setBlockHash(blockHash)
              .setDeltas(tracesList.toFungibleBalanceDeltas())
              .build()

          }
        }
      }.toTopic(CanonicalTracesEtherDeltas)

    mapToFungibleBalanceDeltas(CanonicalTracesEtherDeltas.stream(builder))

  }

  private fun etherDeltasForFees(builder: StreamsBuilder) {

    val txFeesStream = CanonicalTransactionFees.stream(builder)

    txFeesStream
      .mapValues { _, feeList ->

        if (feeList != null) {
          FungibleBalanceDeltaListRecord.newBuilder()
            .setBlockHash(feeList.getBlockHash())
            .setDeltas(feeList.toEtherBalanceDeltas())
            .build()
        } else {
          // pass along the tombstone
          null
        }

      }.toTopic(CanonicalTransactionFeesEtherDeltas)

    mapToFungibleBalanceDeltas(CanonicalTransactionFeesEtherDeltas.stream(builder))

    CanonicalBlockAuthors.stream(builder)
      .join(
        txFeesStream,
        { left, right ->

          if (left.getBlockHash() != right.getBlockHash()) {

            // We're in the middle of an update/fork so we publish a tombstone
            null

          } else {

            val totalTxFees = right.getTransactionFees()
              .map { it.getTransactionFeeBI() }
              .fold(BigInteger.ZERO) { memo, next -> memo + next }

            FungibleBalanceDeltaRecord.newBuilder()
              .setTokenType(FungibleTokenType.ETHER)
              .setDeltaType(FungibleBalanceDeltaType.MINER_FEE)
              .setTraceLocation(
                TraceLocationRecord.newBuilder()
                  .setBlockNumber(left.getBlockNumber())
                  .setBlockHash(left.getBlockHash())
                  .build()
              )
              .setAddress(left.getAuthor())
              .setAmountBI(totalTxFees)
              .build()

          }

        },
        JoinWindows.of(Duration.ofHours(2)),
        Joined.with(Serdes.CanonicalKey(), Serdes.BlockAuthor(), Serdes.TransactionFeeList())
      ).toTopic(CanonicalMinerFeesEtherDeltas)


    CanonicalMinerFeesEtherDeltas.stream(builder)
      .mapValues { v ->

        if (v != null) {
          FungibleBalanceDeltaListRecord.newBuilder()
            .setBlockHash(v.getTraceLocation().getBlockHash())
            .setDeltas(listOf(v))
            .build()
        } else {
          null
        }

      }
      .groupByKey()
      .reduce(
        { agg, next ->

          if (next!!.getBlockHash() == agg!!.getBlockHash()) {

            // an update has been published for a previously seen block
            // we assume no material change and therefore emit an event which will have no impact on the balances

            FungibleBalanceDeltaListRecord.newBuilder(agg)
              .setApply(false)
              .build()

          } else {

            // reverse previous deltas

            FungibleBalanceDeltaListRecord.newBuilder()
              .setBlockHash(next.getBlockHash())
              .setApply(true)
              .setDeltas(next.getDeltas())
              .setReversals(agg.getDeltas().map { it.reverse() })
              .build()

          }
        },
        Materialized.with(Serdes.CanonicalKey(), Serdes.FungibleBalanceDeltaList())
      )
      .toStream()
      .flatMap { _, v ->

        if (v!!.getApply()) {

          (v.getDeltas() + v.getReversals())
            .map { delta ->
              KeyValue(
                FungibleBalanceKeyRecord.newBuilder()
                  .setAddress(delta.getAddress())
                  .build(),
                FungibleBalanceDeltaRecord.newBuilder(delta)
                  .setAddress(null)
                  .build()
              )
            }

        } else {
          emptyList()
        }

      }.toTopic(FungibleBalanceDeltas)
  }

  private fun erc20DeltasForReceipts(builder: StreamsBuilder) {

    CanonicalReceipts.stream(builder)
      .mapValues { _, v ->

        when (v) {
          null -> null
          else -> {

            // filter out receipts with ERC20 related logs

            val blockHash = v.getReceipts().firstOrNull()?.getBlockHash()

            val receiptsWithErc20Logs = v.getReceipts()
              .filter { receipt ->

                val logs = receipt.getLogs()

                when(logs.isEmpty()) {
                  true -> false
                  else ->
                    logs
                      .map { log -> ERC20Abi.matchEventHex(log.getTopics()).isDefined() }
                      .reduce { a, b -> a || b }
                }

              }

            val deltas = receiptsWithErc20Logs
              .flatMap { receipt ->

                val traceLocation = TraceLocationRecord.newBuilder()
                  .setBlockNumber(receipt.getBlockNumber())
                  .setBlockHash(receipt.getBlockHash())
                  .setTransactionHash(receipt.getTransactionHash())
                  .build()

                receipt.getLogs()
                  .map { log -> ERC20Abi.decodeTransferEventHex(log.getData(), log.getTopics()) }
                  .mapNotNull { transferOpt -> transferOpt.orNull() }
                  .flatMap { transfer ->

                    listOf(
                      FungibleBalanceDeltaRecord.newBuilder()
                        .setTokenType(FungibleTokenType.ERC20)
                        .setDeltaType(FungibleBalanceDeltaType.TOKEN_TRANSFER)
                        .setTraceLocation(traceLocation)
                        .setAddress(transfer.from)
                        .setContractAddress(receipt.getTo())
                        .setAmountBI(transfer.amount.negate())
                        .build(),
                      FungibleBalanceDeltaRecord.newBuilder()
                        .setTokenType(FungibleTokenType.ERC20)
                        .setDeltaType(FungibleBalanceDeltaType.TOKEN_TRANSFER)
                        .setTraceLocation(traceLocation)
                        .setAddress(transfer.to)
                        .setContractAddress(receipt.getTo())
                        .setAmountBI(transfer.amount)
                        .build()
                    )

                  }

              }

            FungibleBalanceDeltaListRecord.newBuilder()
              .setBlockHash(blockHash)
              .setDeltas(deltas)
              .build()

          }
        }

      }.toTopic(CanonicalReceiptErc20Deltas)

    mapToFungibleBalanceDeltas(CanonicalReceiptErc20Deltas.stream(builder))

  }

  private fun mapToFungibleBalanceDeltas(stream: KStream<CanonicalKeyRecord, FungibleBalanceDeltaListRecord>) {

    stream
      .groupByKey(Grouped.with(Serdes.CanonicalKey(), Serdes.FungibleBalanceDeltaList()))
      .reduce(
        { agg, next ->

          if (next.getBlockHash() == agg.getBlockHash()) {

            // an update has been published for a previously seen block
            // we assume no material change and therefore emit an event which will have no impact on the balances

            logger.warn { "Update received. Agg = $agg, next = $next" }

            FungibleBalanceDeltaListRecord.newBuilder(agg)
              .setApply(false)
              .build()

          } else {

            // reverse previous deltas

            FungibleBalanceDeltaListRecord.newBuilder()
              .setBlockHash(next.getBlockHash())
              .setDeltas(next.getDeltas())
              .setReversals(agg.getDeltas().map { it.reverse() })
              .build()

          }

        },
        Materialized.with(Serdes.CanonicalKey(), Serdes.FungibleBalanceDeltaList())
      ).toStream()
      .flatMap { _, v ->

        if (v.getApply()) {

          (v.getDeltas() + v.getReversals())
            .map { delta ->
              KeyValue(
                FungibleBalanceKeyRecord.newBuilder()
                  .setAddress(delta.getAddress())
                  .setContract(delta.getContractAddress())
                  .build(),
                FungibleBalanceDeltaRecord.newBuilder(delta)
                  .setAddress(null)
                  .setContractAddress(null)
                  .build()
              )
            }

        } else {
          emptyList()
        }

      }.toTopic(FungibleBalanceDeltas)

  }

  override fun start(cleanUp: Boolean) {
    logger.info { "Starting ${this.javaClass.simpleName}..." }
    super.start(cleanUp)
  }
}
