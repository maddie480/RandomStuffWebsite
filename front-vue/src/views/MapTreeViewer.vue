<template>
  <div class="map-tree-viewer">
    <h1>Celeste Map Tree Viewer</h1>

    <p>
      Use this tool to view the raw contents of your Celeste map as a tree. This
      can be useful to find out where an entity is hiding, or to learn about
      what is actually inside a Celeste map bin!
    </p>

    <p>
      Upload your map bin here, or select a converted JSON file to turn it back
      into a map bin:
    </p>

    <label class="btn btn-default">
      <input
        type="file"
        accept=".bin,.json"
        :disabled="loading"
        @change="selectFile"
      />
    </label>

    <button
      class="btn btn-primary"
      :disabled="!selectedFiles || loading || isJSONFileSelected"
      @click="upload"
    >
      Upload
    </button>

    <button
      class="btn btn-secondary"
      :disabled="!selectedFiles || loading"
      @click="toJSON"
    >
      Convert to {{ isJSONFileSelected ? "BIN" : "JSON" }}
    </button>

    <div class="form-check">
      <input
        id="pretty-print"
        v-model="prettyPrint"
        :disabled="isJSONFileSelected"
        class="form-check-input"
        type="checkbox"
      />
      <label
        class="form-check-label"
        for="pretty-print"
        title="Formats the JSON file, making the file easier to read, but increasing its size."
      >
        Pretty print
      </label>
    </div>

    <div v-if="error" class="error">
      <div class="warning">
        An error occurred. Check that you uploaded a valid Celeste map.
      </div>
    </div>

    <div v-if="loading" class="loading">Loading...</div>

    <form v-if="mapContents !== null" @submit="searchTriggered">
      <input
        v-model="highlightLive"
        class="search form-control"
        placeholder="Search for an entity, trigger, decal or styleground..."
      />
    </form>

    <div v-if="highlight.length !== 0" class="searching">
      Currently searching: <span class="search">{{ highlight }}</span>
    </div>

    <div v-if="mapContents !== null" class="search-options">
      <div class="form-check">
        <input
          id="only-show-highlight"
          v-model="onlyShowHighlight"
          class="form-check-input"
          type="checkbox"
        />
        <label class="form-check-label" for="only-show-highlight">
          Only show search results
        </label>
      </div>

      <div class="form-check">
        <input
          id="highlight-oob"
          v-model="outOfBoundsOnly"
          class="form-check-input"
          type="checkbox"
        />
        <label class="form-check-label" for="highlight-oob">
          Out-of-bounds only
        </label>
      </div>
    </div>

    <MapItemCollapsible
      v-if="mapContents !== null"
      class="map-tree"
      :item="mapContents"
      :parent="{ name: 'none' }"
      :grandparent="{ name: 'none' }"
      :highlight="highlight"
      :only-show-highlight="onlyShowHighlight"
      :out-of-bounds-only="outOfBoundsOnly"
    />
  </div>
</template>

<script>
import axios from "axios";
import config from "../config";
import MapItemCollapsible from "../components/MapItemCollapsible.vue";
import download from "downloadjs";

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
    prettyPrint: false,
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
            this.selectedFiles[0],
          )
        ).data;

        this.mapContents = contents;
      } catch {
        this.error = true;
      }
      this.loading = false;
    },
    toJSON: async function () {
      try {
        this.loading = true;
        this.error = false;
        this.mapContents = null;
        this.highlight = "";
        this.highlightLive = "";

        const endpoint = this.isJSONFileSelected
          ? "json-to-bin"
          : "bin-to-json";

        const resultContents = (
          await axios.post(
            `${config.backendUrl}/celeste/${endpoint}`,
            this.selectedFiles[0],
            { responseType: this.isJSONFileSelected ? "blob" : "json" },
          )
        ).data;

        if (this.isJSONFileSelected) {
          download(resultContents, "map.bin", "application/octet-stream");
        } else {
          download(
            JSON.stringify(resultContents, undefined, this.prettyPrint ? 2 : 0),
            "map.json",
            "application/json",
          );
        }
      } catch {
        this.error = true;
      }

      this.loading = false;
    },
  },
  computed: {
    isJSONFileSelected() {
      return (
        this.selectedFiles &&
        this.selectedFiles[0].name.toLowerCase().endsWith(".json")
      );
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

.form-check {
  margin: 10px;
  user-select: none;
  display: inline-block;
}

input.search {
  margin-top: 30px;
}

button.btn-secondary {
  margin-left: 10px;
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
