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

      <div class="checkbox-options">
        <div class="form-check">
          <input
            class="form-check-input"
            type="checkbox"
            v-model="groupAssets"
            v-on:change="filterList"
            id="group-assets"
          />
          <label class="form-check-label" for="group-assets">
            Group assets belonging to the same animation
          </label>
        </div>
        <div class="form-check">
          <input
            class="form-check-input"
            type="checkbox"
            v-model="zoom"
            id="zoom-checkbox"
          />
          <label class="form-check-label" for="zoom-checkbox"> 2x zoom </label>
        </div>
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

      <div id="asset-browser" class="row">
        <AssetDriveItem
          v-for="item in page"
          v-bind:key="item.id"
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
        // search
        .filter((l) =>
          l.name.toLowerCase().includes(this.currentSearch.toLowerCase()),
        )
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
  mounted: async function () {
    await this.reload();
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
