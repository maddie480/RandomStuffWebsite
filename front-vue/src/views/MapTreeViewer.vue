<template>
  <div class="map-tree-viewer">
    <h1>Celeste Map Tree Viewer</h1>

    <p>
      Use this tool to view the raw contents of your Celeste map as a tree. This
      can be useful to find out where an entity is hiding, or to learn about
      what is actually inside a Celeste map bin!
    </p>

    <p>Upload your map bin here:</p>

    <label class="btn btn-default">
      <input
        type="file"
        accept=".bin"
        @change="selectFile"
        :disabled="loading"
      />
    </label>

    <button
      class="btn btn-success"
      :disabled="!selectedFiles || loading"
      @click="upload"
    >
      Upload
    </button>

    <div class="error" v-if="error">
      <div class="warning">
        An error occurred. Check that you uploaded a valid Celeste map.
      </div>
    </div>

    <div class="loading" v-if="loading">Loading...</div>

    <form v-on:submit="searchTriggered" v-if="mapContents !== null">
      <input
        v-model="highlightLive"
        class="search form-control"
        placeholder="Search for an entity, trigger, decal or stylegrounds..."
      />
    </form>

    <div class="searching" v-if="highlight.length !== 0">
      Currently searching: <span class="search">{{ highlight }}</span>
    </div>

    <div class="search-options" v-if="mapContents !== null">
      <div class="form-check">
        <input
          class="form-check-input"
          type="checkbox"
          v-model="onlyShowHighlight"
          id="only-show-highlight"
        />
        <label class="form-check-label" for="only-show-highlight">
          Only show search results
        </label>
      </div>

      <div class="form-check">
        <input
          class="form-check-input"
          type="checkbox"
          v-model="outOfBoundsOnly"
          id="highlight-oob"
        />
        <label class="form-check-label" for="highlight-oob">
          Out-of-bounds only
        </label>
      </div>
    </div>

    <MapItemCollapsible
      class="map-tree"
      :item="mapContents"
      :parent="{ name: 'none' }"
      :grandparent="{ name: 'none' }"
      :highlight="highlight"
      :onlyShowHighlight="onlyShowHighlight"
      :outOfBoundsOnly="outOfBoundsOnly"
      v-if="mapContents !== null"
    />
  </div>
</template>

<script>
import axios from "axios";
import config from "../config";
import MapItemCollapsible from "../components/MapItemCollapsible.vue";

const vue = {
  name: "map-tree-viewer",
  components: { MapItemCollapsible },
  data: () => ({
    selectedFiles: undefined,
    loading: false,
    error: false,
    mapContents: null,
    highlight: "",
    highlightLive: "",
    onlyShowHighlight: false,
    outOfBoundsOnly: false,
  }),
  methods: {
    selectFile: function (e) {
      this.selectedFiles = e.target.files;
    },
    searchTriggered: function (e) {
      this.highlight = this.highlightLive;
      e.preventDefault();
    },
    upload: async function () {
      try {
        this.loading = true;
        this.error = false;
        this.mapContents = null;
        this.highlight = "";
        this.highlightLive = "";

        const contents = (
          await axios.post(
            `${config.backendUrl}/celeste/bin-to-json`,
            this.selectedFiles[0]
          )
        ).data;

        this.mapContents = contents;
      } catch {
        this.error = true;
      }
      this.loading = false;
    },
  },
};

export default vue;
</script>

<style lang="scss" scoped>
h1 {
  margin-bottom: 30px;
}

.loading {
  margin-top: 40px;
  font-size: 16pt;
}

.error {
  font-size: 18pt;
  color: #ff8000;
  margin: 20px;
}

@media (prefers-color-scheme: dark) {
  input[type="file"] {
    color: #dedad6;
  }
}

input.search {
  margin-top: 30px;
}

.searching {
  text-align: left;
  margin-top: 5px;
  font-style: italic;
  color: gray;

  .search {
    font-weight: bold;
  }
}

.search-options {
  text-align: left;
  margin-top: 10px;
  margin-bottom: 20px;
}

@media (min-width: 576px) {
  .form-check {
    display: inline-block;
    margin-right: 20px;
  }
}

.map-tree {
  text-align: left;
  margin-bottom: 30px;
}
</style>
