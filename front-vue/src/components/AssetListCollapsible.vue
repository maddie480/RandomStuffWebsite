<template>
  <div class="asset-list-collapsible">
    <div class="accordion">
      <div class="accordion-item">
        <h2 class="accordion-header" :id="'category-heading-' + categoryId">
          <button
            :class="'accordion-button' + (expanded ? '' : ' collapsed')"
            type="button"
            data-bs-toggle="collapse"
            :aria-expanded="expanded"
            :aria-controls="'category-' + categoryId"
            v-on:click="expandOrCollapse"
          >
            {{ categoryName }}
          </button>
        </h2>
        <div
          :id="'category-' + categoryId"
          class="accordion-collapse collapse show"
          v-if="expanded"
          :aria-labelledby="'category-heading-' + categoryId"
        >
          <div class="accordion-body">
            This will eventually contain {{ categoryId }}.
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: ["categoryName", "categoryId"],
  data: () => ({
    expanded: false,
  }),
  methods: {
    expandOrCollapse: function () {
      this.expanded = !this.expanded;
    },
  },
};
</script>

<style lang="scss" scoped>
.asset-list-collapsible {
  margin: 10px 0;
}
</style>
