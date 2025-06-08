<template>
  <div>
    <h1>Banana Mirror Browser</h1>

    <div>
      <div class="row mt-4 filters">
        <div
          :class="'mb-3 combo-box col-lg-' + (subcategoryFilterVisible ? 3 : 4)"
        >
          <label for="category" class="form-label">Mod category</label>
          <VueMultiselect
            id="category"
            v-model="categoryFilter"
            track-by="name"
            label="name"
            :options="categories"
            :show-labels="false"
            :searchable="false"
            :allow-empty="false"
            :disabled="query !== '' || loading"
            @select="categoryFilterChanged"
          >
          </VueMultiselect>
        </div>
        <div v-if="subcategoryFilterVisible" class="mb-3 combo-box col-lg-3">
          <label for="category" class="form-label">Mod subcategory</label>
          <VueMultiselect
            id="category"
            v-model="subcategoryFilter"
            track-by="name"
            label="name"
            :options="subcategories"
            :show-labels="false"
            :searchable="false"
            :allow-empty="false"
            :disabled="query !== '' || loading"
            @select="subcategoryFilterChanged"
          >
          </VueMultiselect>
        </div>
        <div
          :class="'mb-3 combo-box col-lg-' + (subcategoryFilterVisible ? 3 : 4)"
        >
          <label for="sort" class="form-label">Sort by</label>
          <VueMultiselect
            id="category"
            v-model="sort"
            track-by="id"
            label="name"
            :options="[
              { id: 'latest', name: 'Creation date' },
              { id: 'downloads', name: 'Downloads' },
              { id: 'views', name: 'Views' },
              { id: 'likes', name: 'Likes' },
            ]"
            :show-labels="false"
            :searchable="false"
            :allow-empty="false"
            :disabled="query !== '' || loading"
            @select="sortChanged"
          >
          </VueMultiselect>
        </div>
        <div
          :class="'mb-3 combo-box col-lg-' + (subcategoryFilterVisible ? 3 : 4)"
        >
          <label for="mirror" class="form-label">Mirror to use</label>
          <VueMultiselect
            id="mirror"
            v-model="mirror"
            track-by="id"
            label="name"
            :options="[
              { id: 'jade', name: 'Germany (0x0a.de)' },
              { id: 'wegfan', name: 'China (weg.fan)' },
              { id: 'otobot', name: 'North America (celestemods.com)' },
            ]"
            :show-labels="false"
            :searchable="false"
            :allow-empty="false"
            :disabled="loading"
          >
          </VueMultiselect>
        </div>
      </div>

      <form @submit="searchTriggered">
        <input
          v-model="query"
          class="search form-control"
          placeholder="Search for a mod..."
          :disabled="loading"
        />
      </form>

      <div v-if="loading" class="loading">Loading...</div>
      <div v-else-if="error" class="error">
        <div class="warning">An error occurred.</div>
        <button class="btn btn-warning" @click="reloadPage">Retry</button>
      </div>
      <div v-else>
        <div class="row">
          <div
            v-for="mod in mods"
            :key="mod.id"
            class="col-xl-4 col-md-6 col-12"
          >
            <ModListItem :mod="mod" :mirror="mirror.id" />
          </div>
        </div>
        <ListPaginator
          v-if="totalCount > 0"
          :page-number="page"
          :page-count="pageCount"
          @change-page="changePage"
        />
      </div>
    </div>
  </div>
</template>

<script>
import axios from "axios";
import yaml from "js-yaml";
import config from "../config";
import ModListItem from "../components/ModListItem.vue";
import ListPaginator from "../components/ListPaginator.vue";
import VueMultiselect from "vue-multiselect";

const vue = {
  components: { ModListItem, VueMultiselect, ListPaginator },
  name: "banana-mirror-browser",
  data: () => ({
    page: 1,
    totalCount: 0,
    query: "",
    mods: [],
    mirror: { id: "jade", name: "Germany (0x0a.de)" },
    sort: { id: "latest", name: "Creation date" },
    categoryFilter: { name: "All" },
    subcategoryFilter: { name: "All" },
    categories: [{ name: "All" }],
    subcategories: [{ name: "All" }],
    fullSubcategoryList: [],
    loading: true,
    error: false,
  }),
  methods: {
    changePage: function (newPage) {
      this.page = newPage;
      this.reloadPage();
    },
    searchTriggered: function (event) {
      this.page = 1;
      this.reloadPage();
      event.preventDefault();
    },
    categoryFilterChanged: function (newComboBoxValue) {
      this.page = 1;
      this.refreshSubcategories(newComboBoxValue);
      this.reload(newComboBoxValue, this.subcategoryFilter, this.sort.id);
    },
    subcategoryFilterChanged: function (newComboBoxValue) {
      this.page = 1;
      this.reload(this.categoryFilter, newComboBoxValue, this.sort.id);
    },
    sortChanged: function (newComboBoxValue) {
      this.page = 1;
      this.reload(
        this.categoryFilter,
        this.subcategoryFilter,
        newComboBoxValue.id,
      );
    },
    reloadPage: function () {
      this.reload(this.categoryFilter, this.subcategoryFilter, this.sort.id);
    },
    reload: async function (categoryFilter, subcategoryFilter, sort) {
      try {
        this.loading = true;
        this.error = false;

        const yamlLoad = axios
          .get(`${config.backendUrl}/celeste/everest_update.yaml`)
          .then((result) => yaml.load(result.data));

        let result;

        let categoryFilterId = categoryFilter.categoryid;
        let subcategoryFilterId = subcategoryFilter.id;
        if (
          categoryFilterId === undefined &&
          subcategoryFilterId !== undefined
        ) {
          categoryFilterId = subcategoryFilterId;
          subcategoryFilterId = undefined;
        }

        if (this.query === "") {
          result = await axios.get(
            `${config.backendUrl}/celeste/gamebanana-list?sort=${sort}&page=${this.page}&full=true` +
              (categoryFilter.itemtype !== undefined
                ? `&type=${categoryFilter.itemtype}`
                : "") +
              (categoryFilterId !== undefined
                ? `&category=${categoryFilterId}`
                : "") +
              (subcategoryFilterId !== undefined
                ? `&subcategory=${subcategoryFilterId}`
                : ""),
          );
        } else {
          result = await axios.get(
            `${
              config.backendUrl
            }/celeste/gamebanana-search?q=${encodeURIComponent(
              this.query,
            )}&full=true`,
          );
        }

        const mods = result.data;
        const updaterDatabase = await yamlLoad;

        // use the mod updater database to get the mirrored files and their everest.yaml IDs.
        for (const mod of mods) {
          for (const file of mod.Files) {
            const gameBananaFileId = parseInt(
              file.URL.substr(file.URL.lastIndexOf("/") + 1),
            );
            file.GameBananaFileId = gameBananaFileId;

            for (const updaterEntry of Object.entries(updaterDatabase)) {
              if (updaterEntry[1].GameBananaFileId === gameBananaFileId) {
                file.EverestYamlId = updaterEntry[0];
                break;
              }
            }
          }
        }

        this.loading = false;
        this.totalCount = result.headers["x-total-count"];
        this.mods = mods;
      } catch {
        this.error = true;
        this.loading = false;
      }
    },
    refreshSubcategories: async function (category) {
      this.subcategories = [{ name: "All" }];
      this.subcategoryFilter = { name: "All" };

      if (category.itemtype === undefined) return;

      // also load the category list.
      const gamebananaSubcategories =
        this.fullSubcategoryList[category.itemtype][
          "" + (category.categoryid === undefined ? 0 : category.categoryid)
        ];

      for (const subcategory of gamebananaSubcategories) {
        subcategory.name =
          subcategory.name +
          (subcategory.id !== undefined ? ` (${subcategory.count})` : "");
      }

      this.subcategories = gamebananaSubcategories;
      this.subcategoryFilter = gamebananaSubcategories[0];
    },
  },
  computed: {
    pageCount: function () {
      return Math.max(1, Math.floor((this.totalCount - 1) / 20 + 1));
    },
    subcategoryFilterVisible: function () {
      return this.subcategories.length > 1;
    },
  },
  mounted: async function () {
    this.reloadPage();

    // also load the category list.
    const gamebananaCategories = await axios
      .get(`${config.backendUrl}/celeste/gamebanana-categories`)
      .then((result) => yaml.load(result.data));

    for (const category of gamebananaCategories) {
      category.name = `${category.formatted} (${category.count})`;
    }

    this.categories = gamebananaCategories;
    this.categoryFilter = gamebananaCategories[0];

    this.fullSubcategoryList = await axios
      .get(`${config.backendUrl}/celeste/gamebanana-subcategories`)
      .then((result) => yaml.load(result.data));
  },
};

export default vue;
</script>

<style>
@import "../../node_modules/vue-multiselect/dist/vue-multiselect.css";
</style>

<style scoped lang="scss">
.loading,
.error {
  margin-top: 30px;
  font-size: 16pt;
}
.error {
  color: #ff8000;
}

.paginator {
  font-size: 14pt;
  margin-bottom: 30px;

  .btn {
    margin: 5px 2px;
  }
}

.search {
  margin-top: 20px;
  margin-bottom: 20px;
}

.filters {
  margin-bottom: -16px;
}

.combo-box {
  text-align: left;

  label {
    font-weight: bold;
  }
}
</style>

<style lang="scss">
// do not show text select cursor when hovering the multiselect label
.multiselect__single {
  cursor: default;
  color: rgba(0, 0, 0, 0.9);
  user-select: none;
}

// make multiselect colors match the bootstrap dropdown
.multiselect__option--selected.multiselect__option--highlight {
  background: #0d6efd;
}
.multiselect__option--selected {
  background: #0d6efd;
  font-weight: normal;
  color: #fff;
}
.multiselect__option--highlight {
  background: #e9ecef;
  color: rgba(0, 0, 0, 0.9);
}
.multiselect__tags,
.multiselect__content-wrapper {
  border-color: rgb(206, 212, 218);
}
.multiselect--disabled .multiselect__select {
  background-color: transparent;
}

// multiselect dark theme
@media (prefers-color-scheme: dark) {
  .multiselect__tags,
  .multiselect__single {
    background-color: #000;
    color: #dedad6;
  }
  .multiselect--disabled {
    background-color: #000;
  }
  .multiselect__content {
    background-color: rgb(52, 58, 64);
    color: #dedad6;
  }
  .multiselect__tags,
  .multiselect__content-wrapper {
    border-color: #777;
  }
  .multiselect__option--highlight {
    background-color: rgba(255, 255, 255, 0.15);
    color: #dedad6;
  }
  .multiselect__option--selected {
    background: #0d6efd;
    color: #fff;
  }
}
</style>
