<template>
  <div>
    <div class="loading" v-if="loading">Loading...</div>
    <div class="error" v-else-if="error">
      <div>An error occurred.</div>
      <button class="btn btn-warning" v-on:click="reload">Retry</button>
    </div>
    <div v-else>
      <form v-on:submit="searchTriggered">
        <input
          v-model="query"
          class="search form-control"
          :placeholder="'Filter ' + categoryDisplayName + 's by name...'"
          :disabled="loading"
        />
      </form>

      <div class="searching" v-if="currentSearch.length !== 0">
        Currently searching:
        <span class="current-search">{{ currentSearch }}</span>
      </div>

      <div class="tag-filters" v-if="allTags.length !== 0">
        Tags (click to filter):
        <span
          :class="
            'tag badge bg-' + (tagFilter === null ? 'primary' : 'secondary')
          "
          v-on:click="filterOnTag(null)"
          >Do not filter</span
        >

        <span
          v-for="tag in allTags"
          v-bind:key="tag"
          v-on:click="filterOnTag(tag)"
          :class="
            'tag badge bg-' + (tagFilter === tag ? 'primary' : 'secondary')
          "
          >{{ tag }}</span
        >
      </div>

      <AssetDriveItem
        v-for="item in this.page"
        v-bind:key="item.id"
        :data="item"
        :category-display-name="categoryDisplayName"
      />

      <ListPaginator
        v-if="filteredList.length !== 0"
        :page-number="pageNumber"
        :page-count="pageCount"
        v-on:change-page="changePage"
      />
    </div>
  </div>
</template>

<script>
import axios from "axios";
import config from "../config";
import AssetDriveItem from "./AssetDriveItem.vue";
import ListPaginator from "./ListPaginator.vue";

export default {
  components: { AssetDriveItem, ListPaginator },
  props: ["category", "category-display-name"],
  data: () => ({
    loading: false,
    error: false,
    query: "",
    currentSearch: "",
    allTags: [],
    tagFilter: null,
    fullList: [],
    filteredList: [],
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
        this.allTags = [];

        this.fullList.forEach((element) => {
          if (element.tags === undefined) return;

          element.tags.forEach((tag) => {
            if (!this.allTags.includes(tag)) this.allTags.push(tag);
          });
        });

        this.filterList();

        this.loading = false;
      } catch (e) {
        this.error = true;
        this.loading = false;
      }
    },
    filterList: function () {
      this.filteredList = this.fullList
        .filter((l) =>
          l.name.toLowerCase().includes(this.currentSearch.toLowerCase()),
        )
        .filter((l) => {
          if (this.tagFilter === null) return true;
          if (l.tags === undefined) return false;
          return l.tags.includes(this.tagFilter);
        });

      this.pageCount = Math.floor((this.filteredList.length - 1) / 48) + 1;
      this.changePage(1);
    },
    changePage(newPage) {
      this.pageNumber = newPage;
      const start = (this.pageNumber - 1) * 48;
      this.page = this.filteredList.slice(start, start + 48);
    },
    searchTriggered(event) {
      event.preventDefault();
      this.currentSearch = this.query;
      this.filterList();
    },
    filterOnTag(tag) {
      this.tagFilter = tag;
      this.filterList();
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
  font-size: 20pt;
}
.error {
  color: #ff8000;
}

.search {
  width: calc(100% - 10px);
  margin: 20px 5px;
}

.tag-filters {
  text-align: left;
  margin: -15px 5px 20px 5px;

  .tag {
    margin-left: 2px;
    cursor: default;
  }
}

.searching {
  text-align: left;
  margin: -15px 5px 20px 5px;
  font-style: italic;
  color: gray;

  .current-search {
    font-weight: bold;
  }
}
</style>
