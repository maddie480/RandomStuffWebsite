<template>
  <div>
    <div class="loading" v-if="loading">Loading...</div>
    <div class="error" v-else-if="error">
      <div>An error occurred.</div>
      <button class="btn btn-warning" v-on:click="reload">Retry</button>
    </div>
    <div v-else>
      <AssetDriveItem
        v-for="item in this.page"
        v-bind:key="item.id"
        :data="item"
        :category-display-name="categoryDisplayName"
      />
    </div>
    <div class="paginator">
      <button
        class="btn btn-outline-secondary"
        :disabled="pageNumber <= 1"
        v-on:click="changePage(1)"
      >
        &lt;&lt;
      </button>
      <button
        class="btn btn-outline-secondary"
        :disabled="pageNumber <= 1"
        v-on:click="changePage(pageNumber - 1)"
      >
        &lt;
      </button>
      {{ pageNumber }} / {{ pageCount }}
      <button
        class="btn btn-outline-secondary"
        :disabled="pageNumber >= pageCount"
        v-on:click="changePage(pageNumber + 1)"
      >
        &gt;
      </button>
      <button
        class="btn btn-outline-secondary"
        :disabled="pageNumber >= pageCount"
        v-on:click="changePage(pageCount)"
      >
        &gt;&gt;
      </button>
    </div>
  </div>
</template>

<script>
import axios from "axios";
import config from "../config";
import AssetDriveItem from "../components/AssetDriveItem.vue";

export default {
  components: { AssetDriveItem },
  props: ["category", "category-display-name"],
  data: () => ({
    loading: false,
    error: false,
    fullList: [],
    pageCount: 0,
    page: [],
    pageNumber: 1,
  }),
  methods: {
    reload: async function () {
      try {
        this.loading = true;
        this.error = false;

        this.fullList = (
          await axios.get(
            `${config.backendUrl}/celeste/asset-drive/list/${this.category}`,
          )
        ).data;
        this.pageCount = Math.floor((this.fullList.length - 1) / 48);
        this.changePage(1);

        this.loading = false;
      } catch (e) {
        this.error = true;
        this.loading = false;
      }
    },
    changePage(newPage) {
      this.pageNumber = newPage;
      const start = (this.pageNumber - 1) * 48;
      this.page = this.fullList.slice(start, start + 48);
    },
  },
  mounted: async function () {
    await this.reload();
  },
};
</script>

<style lang="scss" scoped>
.loading,
.error {
  margin-top: 30px;
  font-size: 48pt;
}
.error {
  color: #ff8000;
}

.paginator {
  font-size: 14pt;

  .btn {
    margin: 5px 2px;
  }

  margin-bottom: 30px;
}
</style>
