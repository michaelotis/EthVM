package io.enkrypt.common.extensions

import io.enkrypt.avro.capture.BlockKeyRecord
import io.enkrypt.avro.capture.BlockRecord
import io.enkrypt.avro.capture.TransactionReceiptRecord
import io.enkrypt.avro.capture.TransactionRecord
import io.enkrypt.avro.exchange.ExchangeRateRecord
import io.enkrypt.avro.processing.TokenBalanceKeyRecord
import io.enkrypt.avro.processing.TokenTransferRecord
import java.math.BigInteger
import java.nio.ByteBuffer

fun TokenTransferRecord.isFungible() = !(this.getFrom() == null || this.getTo() == null || this.getAmount() == null || this.getTokenId() != null)

fun TokenTransferRecord.isNonFungible() = !(this.getFrom() == null || this.getTo() == null || this.getTokenId() == null || this.getAmount() != null)

fun TokenBalanceKeyRecord.isFungible() = this.getAddress() != null

fun TokenBalanceKeyRecord.isNonFungible() = this.getTokenId() != null

val TokenTransferRecord.amountBI: BigInteger?
  get() = getAmount().unsignedBigInteger()

fun TokenTransferRecord.Builder.setAmount(amount: BigInteger) =
  this.setAmount(amount.unsignedByteBuffer())

fun TokenTransferRecord.Builder.setTokenId(tokenId: BigInteger) =
  this.setTokenId(tokenId.unsignedByteBuffer())

fun TransactionReceiptRecord.isSuccess(): Boolean {
  // TODO fix me
  return false
}

fun BlockRecord.txFees(): List<BigInteger> {
  // TODO fix me
//  this.getTransactions().zip(this.getTransactionReceipts())
//    .map { (tx, r) -> tx.getGasPrice().unsignedBigInteger()!! * r.getGasUsed().unsignedBigInteger()!! }
  return emptyList()
}

fun BlockRecord.totalTxFees(): BigInteger = this.txFees()
  .fold(0.toBigInteger()) { memo, next -> memo + next }

fun BlockRecord.keyRecord(): BlockKeyRecord =
  BlockKeyRecord.newBuilder()
    .setNumber(getHeader().getNumber())
    .build()

fun TransactionRecord.txFee(receipt: TransactionReceiptRecord): BigInteger =
  getGasPrice().unsignedBigInteger()!! * receipt.getGasUsed().unsignedBigInteger()!!

fun ExchangeRateRecord.isValid() = !(this.marketCap == -1.0 || this.marketCapRank == -1)

object AvroHelpers {

  fun blockKey(number: Long) = blockKey(number.toBigInteger())

  fun blockKey(number: BigInteger): BlockKeyRecord =
    blockKey(number.unsignedByteBuffer())

  fun blockKey(number: ByteBuffer?): BlockKeyRecord =
    BlockKeyRecord.newBuilder()
      .setNumber(number)
      .build()
}
