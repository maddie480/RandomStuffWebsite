<template>
  <div>
    <div v-if="loading" class="loading">Loading...</div>
    <div v-else-if="error" class="error">
      <div>An error occurred.</div>
      <button class="btn btn-warning" @click="reload">Retry</button>
    </div>
    <div v-else>
      <form @submit="searchTriggered">
        <input
          v-model="query"
          class="search form-control"
          :placeholder="
            'Filter ' + categoryDisplayName + 's by name or @author...'
          "
          :disabled="loading"
        />
      </form>

      <div
        v-if="currentSearch.length !== 0 && !currentSearch.startsWith('@')"
        class="searching"
      >
        Currently searching:
        <span class="current-search">{{ currentSearch }}</span>
      </div>
      <div
        v-else-if="currentSearch.length > 1 && currentSearch.startsWith('@')"
        class="searching"
      >
        Currently searching for author:
        <span class="current-search">{{ currentSearch.substring(1) }}</span>
      </div>

      <div class="checkbox-options">
        <div class="form-check">
          <input
            id="group-assets"
            v-model="groupAssets"
            class="form-check-input"
            type="checkbox"
            @change="filterList"
          />
          <label class="form-check-label" for="group-assets">
            Group assets belonging to the same animation
          </label>
        </div>
        <div class="form-check">
          <input
            id="zoom-checkbox"
            v-model="zoom"
            class="form-check-input"
            type="checkbox"
          />
          <label class="form-check-label" for="zoom-checkbox"> 2x zoom </label>
        </div>
      </div>

      <div v-if="allTags.length !== 0" class="tag-filters">
        Tags (click to filter):
        <span
          :class="
            'tag badge bg-' + (tagFilter === null ? 'primary' : 'secondary')
          "
          @click="filterOnTag(null)"
          >Do not filter</span
        >

        <span
          v-for="tag in allTags"
          :key="tag"
          :class="
            'tag badge bg-' + (tagFilter === tag ? 'primary' : 'secondary')
          "
          @click="filterOnTag(tag)"
          >{{ tag }}</span
        >
      </div>

      <div id="asset-browser" class="row">
        <AssetDriveItem
          v-for="item in page"
          :key="item.id"
          :data="item"
          :category-display-name="categoryDisplayName"
          :zoom="zoom"
          :folders="folders"
        />
      </div>

      <ListPaginator
        v-if="filteredList.length !== 0"
        :page-number="pageNumber"
        :page-count="pageCount"
        @change-page="changePage"
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
  props: ["category", "category-display-name", "folders"],
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
    groupAssets: false,
    zoom: false,
  }),
  mounted: async function () {
    await this.reload();
  },
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
      // a query starting with @ indicates we should search by author instead of by name
      const filterFunction = this.currentSearch.startsWith("@")
        ? (l) =>
            l.author
              .toLowerCase()
              .includes(this.currentSearch.substring(1).toLowerCase())
        : (l) =>
            l.name.toLowerCase().includes(this.currentSearch.toLowerCase());

      this.filteredList = this.fullList
        // search by name or author
        .filter(filterFunction)
        // filter by tag
        .filter((l) => {
          if (this.tagFilter === null) return true;
          if (l.tags === undefined) return false;
          return l.tags.includes(this.tagFilter);
        });

      // group
      if (this.groupAssets) {
        const newAssetList = [];
        const assetsByPrefix = {};
        for (let i = 0; i < this.filteredList.length; i++) {
          const item = this.filteredList[i];
          const numberedSuffixRegex = item.name.match(/[0-9]+\.png$/);
          if (numberedSuffixRegex === null) {
            newAssetList.push(item);
            continue;
          }

          const prefix = item.name.substr(
            0,
            item.name.length - numberedSuffixRegex[0].length,
          );
          if (assetsByPrefix[prefix] !== undefined) {
            assetsByPrefix[prefix].frames.push(item);
          } else {
            newAssetList.push(item);
            assetsByPrefix[prefix] = item;
            item.frames = [item];
          }
        }
        this.filteredList = newAssetList;
      }

      this.pageCount = Math.floor((this.filteredList.length - 1) / 48) + 1;
      this.changePage(1, false);
    },
    changePage(newPage, scrollTop = true) {
      this.pageNumber = newPage;
      const start = (this.pageNumber - 1) * 48;
      this.page = this.filteredList.slice(start, start + 48);

      if (scrollTop) {
        setTimeout(
          () =>
            document
              .getElementById("asset-browser")
              .scrollIntoView({ behavior: "smooth" }),
          0,
        );
      }
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
};
</script>

<style lang="scss" scoped>
// loading and error messages
.loading,
.error {
  margin-top: 30px;
  font-size: 20pt;
}
.error {
  color: #ff8000;
}

// search bar
.search {
  width: calc(100% - 10px);
  margin: 20px 5px 5px 5px;
}

.searching {
  text-align: left;
  margin: 5px;
  font-style: italic;
  color: gray;

  .current-search {
    font-weight: bold;
  }
}

// filter by tags
.tag-filters {
  text-align: left;
  margin: -15px 5px 20px 5px;

  .tag {
    margin-left: 2px;
    cursor: default;
  }
}

// checkbox toggles
.checkbox-options {
  text-align: left;
  margin: 0 5px 20px 5px;
}

@media (min-width: 576px) {
  .form-check {
    display: inline-block;
    margin-right: 20px;
  }
}
</style>
