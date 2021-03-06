<template>
    <v-container grid-list-lg class="mb-0">
        <app-bread-crumbs :new-items="crumbs" />
        <app-message :messages="errorMessages" />
        <app-error v-if="hasError" :has-error="hasError" :message="error" />
        <!--
        =====================================================================================
          TX DETAILS LIST
        =====================================================================================
        -->
        <tx-details v-if="isValid && !hasError" :tx-ref="txRef" @errorDetails="setError" />
    </v-container>
</template>

<script lang="ts">
import AppBreadCrumbs from '@app/core/components/ui/AppBreadCrumbs.vue'
import AppError from '@app/core/components/ui/AppError.vue'
import AppMessage from '@app/core/components/ui/AppMessage.vue'
import { eth } from '@app/core/helper'
import { Crumb } from '@app/core/components/props'
import { Vue, Component, Prop } from 'vue-property-decorator'
import TxDetails from '@app/modules/txs/handlers/TxDetails/TxDetails.vue'
import { ErrorMessageTx } from '@app/modules/txs/models/ErrorMessagesForTx'

@Component({
    components: {
        AppBreadCrumbs,
        AppError,
        AppMessage,
        TxDetails
    }
})
export default class PageDetailsTxs extends Vue {
    /*
  ===================================================================================
    Props
  ===================================================================================
  */

    @Prop({ type: String }) txRef!: string

    /*
  ===================================================================================
    Initial Data
  ===================================================================================
  */
    errorMessages: ErrorMessageTx[] = []
    error = ''

    /*
  ===================================================================================
    Lifecycle
  ===================================================================================
  */

    created() {
        // Check that current tx ref is valid one
        if (!this.isValid) {
            this.error = this.$i18n.t('message.invalid.tx').toString()
            return
        }

        window.scrollTo(0, 0)
    }

    /*
  ===================================================================================
    Computed Values
  ===================================================================================
  */

    get isValid(): boolean {
        return eth.isValidHash(this.txRef)
    }

    get hasError(): boolean {
        return this.error !== ''
    }

    /*
 ===================================================================================
   Methods
 ===================================================================================
 */

    /**
     * Returns breadcrumbs entry for this particular view.
     * Required for AppBreadCrumbs
     *
     * @return {Array} - Breadcrumb entry. See description.
     */
    get crumbs(): Crumb[] {
        return [
            {
                text: this.$t('tx.mined'),
                link: '/txs'
            },
            {
                text: this.$tc('tx.hash', 1),
                hash: this.txRef
            }
        ]
    }
    setError(hasError: boolean, message: ErrorMessageTx): void {
        if (hasError) {
            if (message === ErrorMessageTx.notFound) {
                this.error = this.$i18n.t(message).toString()
            } else {
                if (!this.errorMessages.includes(message)) {
                    this.errorMessages.push(message)
                }
            }
        } else {
            if (this.errorMessages.length > 0) {
                const index = this.errorMessages.indexOf(message)
                if (index > -1) {
                    this.errorMessages.splice(index, 1)
                }
            }
        }
    }
}
</script>
