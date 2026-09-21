// Keep the platform informed while the iframe is alive, not only on unload.
module.exports = function configurationSync(taskKey) {
    return {
        data() {
            return { configurationReady: false, configurationInvalid: false }
        },
        watch: {
            [taskKey]: {
                deep: true,
                handler() {
                    if (this.configurationReady) this.publishConfiguration()
                }
            },
            atomValue: {
                deep: true,
                handler() {
                    if (this.configurationReady && !this.atomPropsDisabled) this.setAtomValue()
                }
            }
        },
        mounted() {
            // Component mounted hooks initialize the SCA/SAST local form first.
            this.$nextTick(() => {
                if (this._isDestroyed) return
                this.configurationReady = true
                this.publishConfiguration()
            })
        },
        beforeDestroy() {
            if (this.configurationReady) this.publishConfiguration()
        },
        methods: {
            publishConfiguration() {
                if (this.atomPropsDisabled) return
                const task = this[taskKey]
                Object.keys(task).forEach(key => {
                    if (this.atomValue[key] !== task[key]) this.$set(this.atomValue, key, task[key])
                })
                const valid = this.validateAll ? this.validateAll(true) : this.validate()
                this.configurationInvalid = !valid
                this.setAtomIsError(!valid)
                // No debounce: explicit Save must deliver the current values immediately.
                this.setAtomValue()
            }
        }
    }
}

