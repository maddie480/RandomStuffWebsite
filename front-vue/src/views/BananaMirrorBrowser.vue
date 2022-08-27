<template>
  <div>
    <h1>Banana Mirror Browser</h1>

    <div>
      <div class="row mt-4 filters">
        <div class="mb-3 col-lg-4 combo-box">
          <label for="category" class="form-label">Mod category</label>
          <VueMultiselect
            v-model="categoryFilter"
            track-by="name"
            label="name"
            :options="categories"
            :show-labels="false"
            :searchable="false"
            :allow-empty="false"
            :disabled="query !== '' || loading"
            @select="searchOrFilterStuff"
            id="category"
          >
          </VueMultiselect>
        </div>
        <div class="mb-3 col-lg-4 combo-box">
          <label for="sort" class="form-label">Sort by</label>
          <VueMultiselect
            v-model="sort"
            track-by="sortId"
            label="name"
            :options="[
              { sortId: 'latest', name: 'Creation date' },
              { sortId: 'downloads', name: 'Downloads' },
              { sortId: 'views', name: 'Views' },
              { sortId: 'likes', name: 'Likes' },
            ]"
            :show-labels="false"
            :searchable="false"
            :allow-empty="false"
            :disabled="query !== '' || loading"
            @select="searchOrFilterStuff"
            id="category"
          >
          </VueMultiselect>
        </div>
        <div class="mb-3 col-lg-4 combo-box">
          <label for="mirror" class="form-label">Mirror to use</label>
          <VueMultiselect
            v-model="mirror"
            track-by="id"
            label="name"
            :options="[
              { id: 'jade', name: 'Germany (0x0a.de)' },
              { id: 'wegfan', name: 'China (weg.fan)' },
            ]"
            :show-labels="false"
            :searchable="false"
            :allow-empty="false"
            :disabled="loading"
            id="mirror"
          >
          </VueMultiselect>
        </div>
      </div>

      <form v-on:submit="searchOrFilterStuff">
        <input
          v-model="query"
          class="search form-control"
          placeholder="Search for a mod..."
          :disabled="loading"
        />
      </form>

      <div class="loading" v-if="loading">Loading...</div>
      <div class="error" v-else-if="error">
        <div class="warning">An error occurred.</div>
        <button class="btn btn-warning" v-on:click="reloadPage">Retry</button>
      </div>
      <div v-else>
        <div class="row">
          <div
            v-bind:key="mod.id"
            v-for="mod in mods"
            class="col-xl-4 col-md-6 col-sm-12"
          >
            <ModListItem :mod="mod" :mirror="mirror.id" />
          </div>
        </div>
        <div class="paginator" v-if="this.totalCount > 0">
          <button
            class="btn btn-outline-secondary"
            :disabled="page <= 1"
            v-on:click="firstPage"
          >
            &lt;&lt;
          </button>
          <button
            class="btn btn-outline-secondary"
            :disabled="page <= 1"
            v-on:click="previousPage"
          >
            &lt;
          </button>
          {{ page }} / {{ pageCount }}
          <button
            class="btn btn-outline-secondary"
            :disabled="page >= pageCount"
            v-on:click="nextPage"
          >
            &gt;
          </button>
          <button
            class="btn btn-outline-secondary"
            :disabled="page >= pageCount"
            v-on:click="lastPage"
          >
            &gt;&gt;
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from "axios";
import yaml from "js-yaml";
import config from "../config";
import ModListItem from "../components/ModListItem.vue";
import VueMultiselect from "vue-multiselect";

const vue = {
  components: { ModListItem, VueMultiselect },
  name: "banana-mirror-browser",
  data: () => ({
    page: 1,
    totalCount: 0,
    query: "",
    mods: [],
    mirror: { id: "jade", name: "0x0a.de (Germany)" },
    sort: { sortId: "latest", name: "Creation date" },
    categoryFilter: { name: "All" },
    categories: [{ name: "All" }],
    loading: true,
    error: false,
  }),
  methods: {
    firstPage: function () {
      this.page = 1;
      this.reloadPage();
    },
    previousPage: function () {
      this.page--;
      this.reloadPage();
    },
    nextPage: function () {
      this.page++;
      this.reloadPage();
    },
    lastPage: function () {
      this.page = this.pageCount;
      this.reloadPage();
    },
    searchOrFilterStuff: function (newComboBoxValue) {
      this.page = 1;
      this.reloadPage(newComboBoxValue);
    },
    reloadPage: async function (newComboBoxValue) {
      let categoryFilter = this.categoryFilter;
      let sort = this.sort.sortId;

      // when a combo box is changed, the object is not updated yet.
      // we need to take the new value from the parameter Vue-Multiselect gives us!
      if (newComboBoxValue !== undefined) {
        if (newComboBoxValue.sortId !== undefined) {
          sort = newComboBoxValue.sortId;
        } else if (newComboBoxValue.formatted !== undefined) {
          categoryFilter = newComboBoxValue;
        }
      }

      try {
        this.loading = true;
        this.error = false;

        const yamlLoad = axios
          .get(`${config.backendUrl}/celeste/everest_update.yaml`)
          .then((result) => yaml.load(result.data));

        let result;

        if (this.query === "") {
          result = await axios.get(
            `${config.backendUrl}/celeste/gamebanana-list?sort=${sort}&page=${this.page}&full=true` +
              (categoryFilter.itemtype !== undefined
                ? `&type=${categoryFilter.itemtype}`
                : "") +
              (categoryFilter.categoryid
                ? `&category=${categoryFilter.categoryid}`
                : "")
          );
        } else {
          result = await axios.get(
            `${
              config.backendUrl
            }/celeste/gamebanana-search?q=${encodeURIComponent(
              this.query
            )}&full=true`
          );
        }

        const mods = result.data;
        const updaterDatabase = await yamlLoad;

        // use the mod updater database to get the mirrored files and their everest.yaml IDs.
        for (const mod of mods) {
          for (const file of mod.Files) {
            const gameBananaFileId = parseInt(
              file.URL.substr(file.URL.lastIndexOf("/") + 1)
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
      } catch (e) {
        this.error = true;
        this.loading = false;
      }
    },
  },
  computed: {
    pageCount: function () {
      return Math.max(1, Math.floor((this.totalCount - 1) / 20 + 1));
    },
  },
  mounted: async function () {
    this.reloadPage();

    // also load the category list.
    const gamebananaCategories = await axios
      .get(`${config.backendUrl}/celeste/gamebanana-categories?version=3`)
      .then((result) => yaml.load(result.data));

    for (const category of gamebananaCategories) {
      category.name = `${category.formatted} (${category.count})`;
    }

    this.categories = gamebananaCategories;
    this.categoryFilter = gamebananaCategories[0];
  },
};

export default vue;
</script>

<style scoped lang="scss">
@import "../../node_modules/vue-multiselect/dist/vue-multiselect.css";

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

  .btn {
    margin: 5px 2px;
  }

  margin-bottom: 30px;
}

.search {
  margin-top: 20px;
  margin-bottom: 20px;
}

@media (prefers-color-scheme: dark) {
  .search[disabled] {
    background-color: #444;
  }
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
