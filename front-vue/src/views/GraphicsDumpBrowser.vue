<template>
  <div>
    <h1>Celeste Graphics Dump Browser</h1>

    <div class="alert alert-info">
      You can download the full graphics dump
      <a
        href="https://drive.google.com/file/d/1ITwCI2uJ7YflAG0OwBR4uOUEJBjwTCet/view"
        target="_blank"
        >here</a
      >.
    </div>

    <div>
      <div class="loading" v-if="loading">Loading...</div>
      <div class="error" v-else-if="error">
        <div class="warning">An error occurred.</div>
        <button class="btn btn-warning" v-on:click="reloadPage">Retry</button>
      </div>
      <div class="row" v-else>
        <div class="col-xl-3 col-lg-4 col-sm-6 col-xs-12 column">
          <div class="card tree">
            <div class="card-body">
              <GraphicsDumpFolder
                :folder="folderStructure"
                :selectedFolder="selectedFolder"
                v-on:select-folder="(folder) => (selectedFolder = folder)"
              />
            </div>
          </div>
        </div>
        <div class="col-xl-9 col-lg-8 col-sm-6 col-xs-12 column">
          <div class="row">
            <GraphicsDumpItem
              v-bind:key="file"
              v-for="file in selectedFolder.files"
              :name="file"
              :path="selectedFolder.path + '/' + file + '.png'"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from "axios";
import config from "../config";
import GraphicsDumpFolder from "../components/GraphicsDumpFolder.vue";
import GraphicsDumpItem from "../components/GraphicsDumpItem.vue";

const vue = {
  components: { GraphicsDumpFolder, GraphicsDumpItem },
  name: "banana-mirror-browser",
  data: () => ({
    folderStructure: [],
    selectedFolder: { path: "", files: [] },
    loading: true,
    error: false,
  }),
  methods: {
    async reloadPage() {
      try {
        this.loading = true;
        this.error = false;

        const texturePaths = (
          await axios.get(
            `${config.backendUrl}/vanilla-graphics-dump/list.json`,
          )
        ).data;

        const folderStructure = {
          children: [],
        };

        for (let i = 0; i < texturePaths.length; i++) {
          const texturePathSplit = texturePaths[i].split("/");

          let currentLevel = folderStructure;

          for (let j = 0; j < texturePathSplit.length - 1; j++) {
            const folder = texturePathSplit[j];
            let child = currentLevel.children.filter(
              (item) => item.name === folder,
            );

            if (child.length === 0) {
              // create the child folder
              child = {
                name: folder,
                path: texturePathSplit.slice(0, j + 1).join("/"),
                files: [],
                children: [],
              };

              currentLevel.children.push(child);
              currentLevel = child;
            } else {
              currentLevel = child[0];
            }
          }

          const fileName = texturePathSplit[texturePathSplit.length - 1];
          currentLevel.files.push(fileName.substr(0, fileName.length - 4));
        }

        this.folderStructure = folderStructure.children[0];

        this.loading = false;
      } catch (e) {
        this.error = true;
        this.loading = false;
      }
    },
  },
  mounted: async function () {
    this.reloadPage();
  },
};

export default vue;
</script>

<style scoped lang="scss">
h1 {
  margin-bottom: 30px;
}

.row,
.alert {
  text-align: left;
}

.column {
  margin-bottom: 20px;
}

.tree.card {
  padding: 0;
}
</style>
