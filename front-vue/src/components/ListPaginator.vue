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
    {{ pageNumber }} / {{ pageCount }}
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
</style>
