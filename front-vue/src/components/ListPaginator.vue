<template>
  <div class="paginator">
    <button
      class="btn btn-outline-secondary"
      :disabled="pageNumber <= 1"
      v-on:click="$emit('change-page', 1)"
    >
      &lt;&lt;
    </button>
    <button
      class="btn btn-outline-secondary"
      :disabled="pageNumber <= 1"
      v-on:click="$emit('change-page', pageNumber - 1)"
    >
      &lt;
    </button>
    <form v-on:submit="goToPage">
      <input
        type="number"
        min="1"
        :max="pageCount"
        v-model="gotoPage"
        v-on:blur="gotoPage = pageNumber"
      />
      / {{ pageCount }}
    </form>
    <button
      class="btn btn-outline-secondary"
      :disabled="pageNumber >= pageCount"
      v-on:click="$emit('change-page', pageNumber + 1)"
    >
      &gt;
    </button>
    <button
      class="btn btn-outline-secondary"
      :disabled="pageNumber >= pageCount"
      v-on:click="$emit('change-page', pageCount)"
    >
      &gt;&gt;
    </button>
  </div>
</template>

<script>
export default {
  props: ["page-number", "page-count"],
  data: () => ({
    gotoPage: 0,
  }),
  methods: {
    goToPage(event) {
      event.preventDefault();
      this.$emit("change-page", this.gotoPage);
    },
  },
  watch: {
    pageNumber(page) {
      this.gotoPage = page;
    },
  },
  mounted() {
    this.gotoPage = this.pageNumber;
  },
};
</script>

<style lang="scss" scoped>
.paginator {
  font-size: 14pt;

  .btn {
    margin: 5px 2px;
  }

  margin-bottom: 30px;
}

// inline input used to jump to a specific page
form {
  display: inline-block;
  margin: 0 10px;
  vertical-align: middle;
}

// hide the spinner from the number input: the user should type text and press Enter
input::-webkit-outer-spin-button,
input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
input[type="number"] {
  appearance: textfield;
  width: 40px;
  height: 36px;
  text-align: right;
}
</style>
