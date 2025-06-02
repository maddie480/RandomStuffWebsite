<template>
  <div class="col-xl-3 col-lg-4 col-md-6 col-12 base">
    <div class="card">
      <div class="card-body">
        <div :class="imagePath === downloadPath ? 'image-container' : ''">
          <img
            :src="imagePath"
            :class="zoom ? 'zoomed' : ''"
            :style="zoom ? 'margin: ' + height / 2 + 'px 0;' : ''"
            @load="imageLoaded"
          />
        </div>
        <div class="title">
          <span
            v-for="(part, index) in pathParts"
            :key="`${data.name}/${part}/${index}`"
          >
            <a
              v-if="index !== pathParts.length - 1"
              :href="
                'https://drive.google.com/drive/folders/' +
                folders[parentFolders[index]]
              "
              target="_blank"
            >
              {{ part }}
            </a>
            <span v-else class="file-name">{{ part }}</span>
            <span v-if="index !== pathParts.length - 1" class="secondary">
              /
            </span>
          </span>
        </div>
        <div class="secondary authorcredit">by {{ data.author }}</div>
        <div
          v-if="imagePath === downloadPath && width !== null && height !== null"
          class="secondary authorcredit"
        >
          <span v-if="data.frames !== undefined && data.frames.length > 1"
            ><span class="frame-count">{{ data.frames.length }} frames</span>
            &#x2013;
          </span>
          {{ width }} x {{ height }} pixels
        </div>
        <div class="tags">
          <span
            v-for="(tag, index) in data.tags"
            :key="index + tag"
            class="badge bg-primary"
            >{{ tag }}</span
          >
        </div>
        <a
          v-if="data.frames !== undefined && data.frames.length > 1"
          class="btn btn-primary"
          target="_blank"
          :href="multiDownloadPath"
          >Download all</a
        >
        <a v-else class="btn btn-primary" target="_blank" :href="downloadPath"
          >Download</a
        >
        <button
          v-if="hasMoreInfo"
          class="btn btn-secondary"
          @click="openMoreInfo"
        >
          More info
        </button>
      </div>
    </div>

    <div v-if="moreInfoShown" class="modal fade show" tabindex="-1">
      <div class="modal-dialog modal-lg">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">
              More info on {{ categoryDisplayName }} {{ data.name }}
            </h5>
            <button
              type="button"
              class="btn btn-link close"
              aria-label="Close"
              @click="closeMoreInfo"
            >
              ×
            </button>
          </div>
          <div class="modal-body">
            <div v-if="data.template === 'vanilla'">
              <h3>Tileset Template</h3>
              <div>
                This tileset uses the <b>vanilla template</b>. In order to use
                it, copy it to
                <code
                  >Graphics/Atlases/Gameplay/tilesets/yournickname/campaignname</code
                >
                in your mod, then use this in your tileset XML:
                <pre>
&lt;Tileset id="w" copy="z" path="yournickname/campaignname/{{
                    fileNameWithoutExtension
                  }}" sound="8"/&gt;</pre
                >
              </div>
            </div>
            <div v-else-if="data.template === 'better'">
              <h3>Tileset Template</h3>
              <div>
                This tileset uses <b>jade's better template</b>. In order to use
                it, make sure you have this piece of XML in your tileset XML:
                <pre>
  &lt;Tileset id="y" path="subfolder/betterTemplate"&gt;
    &lt;!-- edges --&gt;
    &lt;!-- top --&gt;
    &lt;set mask="x0x-111-x1x" tiles="6,5; 7,5; 8,5; 9,5"/&gt;
    &lt;!-- bottom --&gt;
    &lt;set mask="x1x-111-x0x" tiles="6,10; 7,10; 8,10; 9,10"/&gt;
    &lt;!-- left --&gt;
    &lt;set mask="x1x-011-x1x" tiles="5,6; 5,7; 5,8; 5,9"/&gt;
    &lt;!-- right --&gt;
    &lt;set mask="x1x-110-x1x" tiles="10,6; 10,7; 10,8; 10,9"/&gt;

    &lt;!-- h pillar == --&gt;
    &lt;set mask="x0x-111-x0x" tiles="2,6; 2,7; 2,8; 2,9"/&gt;
    &lt;!-- v pillar left --&gt;
    &lt;set mask="x0x-011-x0x" tiles="1,6; 1,7; 1,8; 1,9"/&gt;
    &lt;!-- v pillar right --&gt;
    &lt;set mask="x0x-110-x0x" tiles="3,6; 3,7; 3,8; 3,9"/&gt;

    &lt;!-- v pillar || --&gt;
    &lt;set mask="x1x-010-x1x" tiles="6,2; 7,2; 8,2; 9,2"/&gt;
    &lt;!-- v pillar top --&gt;
    &lt;set mask="x0x-010-x1x" tiles="6,1; 7,1; 8,1; 9,1"/&gt;
    &lt;!-- v pillar bottom --&gt;
    &lt;set mask="x1x-010-x0x" tiles="6,3; 7,3; 8,3; 9,3"/&gt;

    &lt;!-- single tiles --&gt;
    &lt;set mask="x0x-010-x0x" tiles="1,1; 2,1; 1,2; 2,2"/&gt;

    &lt;!-- corner top left --&gt;
    &lt;set mask="x0x-011-x1x" tiles="4,4; 5,4; 4,5; 5,5"/&gt;
    &lt;!-- corner top right --&gt;
    &lt;set mask="x0x-110-x1x" tiles="10,4; 11,4; 10,5; 11,5"/&gt;
    &lt;!-- corner bottom left --&gt;
    &lt;set mask="x1x-011-x0x" tiles="4,10; 5,10; 4,11; 5,11"/&gt;
    &lt;!-- corner bottom right --&gt;
    &lt;set mask="x1x-110-x0x" tiles="10,10; 11,10; 10,11; 11,11"/&gt;

    &lt;!-- inside corner top left --&gt;
    &lt;set mask="111-111-110" tiles="1,3"/&gt;
    &lt;!-- inside corner bottom left --&gt;
    &lt;set mask="110-111-111" tiles="1,4"/&gt;
    &lt;!-- inside corner top right --&gt;
    &lt;set mask="111-111-011" tiles="2,3"/&gt;
    &lt;!-- inside corner bottom right --&gt;
    &lt;set mask="011-111-111" tiles="2,4"/&gt;

    &lt;!-- |== --&gt;
    &lt;set mask="110-111-110" tiles="11,7"/&gt;
    &lt;!-- _||_ --&gt;
    &lt;set mask="010-111-111" tiles="7,4"/&gt;
    &lt;!-- ==| --&gt;
    &lt;set mask="011-111-011" tiles="4,7"/&gt;
    &lt;!-- T||T --&gt;
    &lt;set mask="111-111-010" tiles="7,11"/&gt;

    &lt;!-- ???? --&gt;
    &lt;set mask="010-111-110" tiles="3,2"/&gt;
    &lt;!-- ???? --&gt;
    &lt;set mask="010-111-011" tiles="4,2"/&gt;
    &lt;!-- ???? --&gt;
    &lt;set mask="011-111-010" tiles="4,1"/&gt;
    &lt;!-- ???? --&gt;
    &lt;set mask="110-111-010" tiles="3,1"/&gt;
    &lt;!-- ???? --&gt;
    &lt;set mask="010-111-010" tiles="3,3"/&gt;
    &lt;!-- ???? --&gt;
    &lt;set mask="110-111-011" tiles="3,4"/&gt;
    &lt;!-- ???? --&gt;
    &lt;set mask="011-111-110" tiles="4,3"/&gt;

    &lt;set mask="padding" tiles="6,6; 7,6; 8,6; 9,6;  6,7; 6,8; 6,9;  9,7; 9,8; 9,9;  7,9; 8,9"/&gt;
    &lt;set mask="center" tiles="7,7; 8,7; 7,8; 8,8"/&gt;
  &lt;/Tileset&gt;</pre
                >

                Then, copy the tileset image to
                <code
                  >Graphics/Atlases/Gameplay/tilesets/yournickname/campaignname</code
                >
                in your mod, then copy this in your tileset XML:
                <pre>
&lt;Tileset id="w" copy="y" path="yournickname/campaignname/{{
                    fileNameWithoutExtension
                  }}" sound="8"/&gt;</pre
                >
              </div>
            </div>
            <div v-else-if="data.template === 'alternate'">
              This tileset uses <b>pixelator's alternate template</b>. In order
              to use it, make sure you have this piece of XML in your tileset
              XML:
              <pre>
  &lt;Tileset id="y" path="alternateTemplate"&gt;
    &lt;!-- edges --&gt;
    &lt;!-- top --&gt;
    &lt;set mask="x0x-111-x1x" tiles="6,5; 7,5; 8,5; 9,5"/&gt;
    &lt;!-- bottom --&gt;
    &lt;set mask="x1x-111-x0x" tiles="6,10; 7,10; 8,10; 9,10"/&gt;
    &lt;!-- left --&gt;
    &lt;set mask="x1x-011-x1x" tiles="5,6; 5,7; 5,8; 5,9"/&gt;
    &lt;!-- right --&gt;
    &lt;set mask="x1x-110-x1x" tiles="10,6; 10,7; 10,8; 10,9"/&gt;

    &lt;!-- h pillar == --&gt;
    &lt;set mask="x0x-111-x0x" tiles="2,6; 2,7; 2,8; 2,9"/&gt;
    &lt;!-- v pillar left --&gt;
    &lt;set mask="x0x-011-x0x" tiles="1,6; 1,7; 1,8; 1,9"/&gt;
    &lt;!-- v pillar right --&gt;
    &lt;set mask="x0x-110-x0x" tiles="3,6; 3,7; 3,8; 3,9"/&gt;

    &lt;!-- v pillar || --&gt;
    &lt;set mask="x1x-010-x1x" tiles="6,2; 7,2; 8,2; 9,2"/&gt;
    &lt;!-- v pillar top --&gt;
    &lt;set mask="x0x-010-x1x" tiles="6,1; 7,1; 8,1; 9,1"/&gt;
    &lt;!-- v pillar bottom --&gt;
    &lt;set mask="x1x-010-x0x" tiles="6,3; 7,3; 8,3; 9,3"/&gt;

    &lt;!-- single tiles --&gt;
    &lt;set mask="x0x-010-x0x" tiles="1,1; 2,1; 1,2; 2,2"/&gt;

    &lt;!-- corner top left --&gt;
    &lt;set mask="x0x-011-x1x" tiles="4,4; 5,4; 4,5; 5,5"/&gt;
    &lt;!-- corner top right --&gt;
    &lt;set mask="x0x-110-x1x" tiles="10,4; 11,4; 10,5; 11,5"/&gt;
    &lt;!-- corner bottom left --&gt;
    &lt;set mask="x1x-011-x0x" tiles="4,10; 5,10; 4,11; 5,11"/&gt;
    &lt;!-- corner bottom right --&gt;
    &lt;set mask="x1x-110-x0x" tiles="10,10; 11,10; 10,11; 11,11"/&gt;

    &lt;!-- inside corner top left --&gt;
    &lt;set mask="111-111-110" tiles="1,3"/&gt;
    &lt;!-- inside corner bottom left --&gt;
    &lt;set mask="110-111-111" tiles="1,4"/&gt;
    &lt;!-- inside corner top right --&gt;
    &lt;set mask="111-111-011" tiles="2,3"/&gt;
    &lt;!-- inside corner bottom right --&gt;
    &lt;set mask="011-111-111" tiles="2,4"/&gt;

    &lt;!-- |== --&gt;
    &lt;set mask="110-111-110" tiles="11,7"/&gt;
    &lt;!-- _||_ --&gt;
    &lt;set mask="010-111-111" tiles="7,4"/&gt;
    &lt;!-- ==| --&gt;
    &lt;set mask="011-111-011" tiles="4,7"/&gt;
    &lt;!-- T||T --&gt;
    &lt;set mask="111-111-010" tiles="7,11"/&gt;

    &lt;!-- ???? --&gt;
    &lt;set mask="010-111-110" tiles="3,2"/&gt;
    &lt;!-- ???? --&gt;
    &lt;set mask="010-111-011" tiles="4,2"/&gt;
    &lt;!-- ???? --&gt;
    &lt;set mask="011-111-010" tiles="4,1"/&gt;
    &lt;!-- ???? --&gt;
    &lt;set mask="110-111-010" tiles="3,1"/&gt;
    &lt;!-- ???? --&gt;
    &lt;set mask="010-111-010" tiles="3,3"/&gt;
    &lt;!-- ???? --&gt;
    &lt;set mask="110-111-011" tiles="3,4"/&gt;
    &lt;!-- ???? --&gt;
    &lt;set mask="011-111-110" tiles="4,3"/&gt;

    &lt;!-- ???? --&gt;
    &lt;set mask="x0x-111-011" tiles="2,10"/&gt;
    &lt;!-- ???? --&gt;
    &lt;set mask="x0x-111-110" tiles="1,10"/&gt;
    &lt;!-- ???? --&gt;
    &lt;set mask="011-111-x0x" tiles="2,11"/&gt;
    &lt;!-- ???? --&gt;
    &lt;set mask="110-111-x0x" tiles="1,11"/&gt;
    &lt;!-- ???? --&gt;
    &lt;set mask="x11-011-x10" tiles="10,1"/&gt;
    &lt;!-- ???? --&gt;
    &lt;set mask="11x-110-01x" tiles="11,1"/&gt;
    &lt;!-- ???? --&gt;
    &lt;set mask="x10-011-x11" tiles="10,2"/&gt;
    &lt;!-- ???? --&gt;
    &lt;set mask="01x-110-11x" tiles="11,2"/&gt;

    &lt;!-- ???? --&gt;
    &lt;set mask="x0x-111-010" tiles="8,11"/&gt;
    &lt;!-- ???? --&gt;
    &lt;set mask="010-111-x0x" tiles="8,4"/&gt;
    &lt;!-- ???? --&gt;
    &lt;set mask="01x-110-01x" tiles="4,8"/&gt;
    &lt;!-- ???? --&gt;
    &lt;set mask="x10-011-x10" tiles="11,8"/&gt;

    &lt;!-- ???? --&gt;
    &lt;set mask="x0x-011-x10" tiles="6,4"/&gt;
    &lt;!-- ???? --&gt;
    &lt;set mask="x0x-110-01x" tiles="9,4"/&gt;
    &lt;!-- ???? --&gt;
    &lt;set mask="x10-011-x0x" tiles="6,11"/&gt;
    &lt;!-- ???? --&gt;
    &lt;set mask="01x-110-x0x" tiles="9,11"/&gt;

    &lt;set mask="padding" tiles="6,6; 7,6; 8,6; 9,6;  6,7; 6,8; 6,9;  9,7; 9,8; 9,9;  7,9; 8,9"/&gt;
    &lt;set mask="center" tiles="7,7; 8,7; 7,8; 8,8"/&gt;
  &lt;/Tileset&gt;</pre
              >

              Then, copy the tileset image to
              <code
                >Graphics/Atlases/Gameplay/tilesets/yournickname/campaignname</code
              >
              in your mod, then copy this in your tileset XML:
              <pre>
&lt;Tileset id="w" copy="y" path="yournickname/campaignname/{{
                  fileNameWithoutExtension
                }}" sound="8"/&gt;</pre
              >
            </div>
            <div v-else-if="data.template !== undefined">
              <h3>Tileset Template</h3>
              This tileset uses a <b>custom template</b>. In order to use it,
              make sure you have this piece of XML in your tileset XML:
              <pre>{{ data.template }}</pre>

              Then, copy the tileset image to
              <code
                >Graphics/Atlases/Gameplay/tilesets/yournickname/campaignname</code
              >
              in your mod, then copy this in your tileset XML:
              <pre>
&lt;Tileset id="w" copy="y" path="yournickname/campaignname/{{
                  fileNameWithoutExtension
                }}" sound="8"/&gt;</pre
              >
            </div>

            <div v-if="data.notes !== undefined">
              <h3>Author's Notes on this {{ categoryDisplayName }}</h3>
              <span class="author-notes">{{ data.notes }}</span>
            </div>
            <div v-if="readme !== null">
              <h3>
                Author's Notes on this group of {{ categoryDisplayName }}s
              </h3>
              <span class="author-notes">{{ readme }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div v-if="moreInfoShown" class="modal-backdrop fade show" />
  </div>
</template>

<script>
import axios from "axios";
import config from "../config";

export default {
  props: {
    data: { type: Object, required: true },
    categoryDisplayName: { type: Object, required: true },
    zoom: { type: Boolean, required: true },
    folders: { type: Array, required: true },
  },
  data: () => ({
    moreInfoShown: false,
    readme: null,
    width: null,
    height: null,
  }),
  computed: {
    imagePath() {
      const fileId =
        this.data.preview !== undefined ? this.data.preview : this.data.id;
      return config.backendUrl + "/celeste/asset-drive/files/" + fileId;
    },
    downloadPath() {
      return config.backendUrl + "/celeste/asset-drive/files/" + this.data.id;
    },
    multiDownloadPath() {
      return (
        config.backendUrl +
        "/celeste/asset-drive/multi-download?files=" +
        this.data.frames.map((f) => f.id).join(",")
      );
    },
    hasMoreInfo() {
      return (
        this.data.template !== undefined ||
        this.data.notes !== undefined ||
        this.data.readme !== undefined
      );
    },
    fileNameWithoutExtension() {
      if (!this.data.name.endsWith(".png")) return "filename";
      return this.data.name.substring(
        this.data.name.lastIndexOf("/") + 1,
        this.data.name.lastIndexOf("."),
      );
    },
    pathParts() {
      return this.data.name.split("/");
    },
    parentFolders() {
      const fullFolderPathParts = this.data.folder.split("/");
      const displayedFolderPathParts = this.data.name.split("/");

      // ditch the file name
      displayedFolderPathParts.splice(displayedFolderPathParts.length - 1, 1);

      // start with pathSoFar being set to the "hidden" part of the path
      let pathSoFar = "";
      for (
        let i = 0;
        i < fullFolderPathParts.length - displayedFolderPathParts.length;
        i++
      ) {
        if (i !== 0) pathSoFar += "/";
        pathSoFar += fullFolderPathParts[i];
      }

      let result = [];

      // then return 1 element per "shown" part of the path, except the file name that we removed earlier
      for (let i = 0; i < displayedFolderPathParts.length; i++) {
        pathSoFar += "/" + this.pathParts[i];
        result.push(pathSoFar);
      }

      return result;
    },
  },
  methods: {
    openMoreInfo: async function () {
      this.moreInfoShown = true;
      if (this.data.readme !== undefined && this.readme === null) {
        try {
          this.readme = (
            await axios.get(
              config.backendUrl +
                "/celeste/asset-drive/files/" +
                this.data.readme,
            )
          ).data;
        } catch {
          this.readme = null;
        }
      }
    },
    closeMoreInfo: function () {
      this.moreInfoShown = false;
    },
    imageLoaded(event) {
      this.width = event.target.width;
      this.height = event.target.height;
    },
  },
};
</script>

<style lang="scss" scoped>
// base card style
.card {
  margin: 0 5px 10px 5px;
}
.base {
  margin: 0;
  padding: 0;
  display: inline-block;
}

// card image
.image-container {
  text-align: center;
  background: url("../../public/img/checkerboard.png");
  padding: 10px;
}
img {
  max-width: 100%;
  max-height: 400px;
}
.zoomed {
  transform: scale(2);
  image-rendering: pixelated;
  max-width: 50%;
  max-height: 200px;
}

// card text: base
.title {
  margin-top: 10px;
}

// file name
.file-name {
  font-weight: bold;
}

// secondary text
.authorcredit {
  font-style: italic;
}
.secondary {
  color: #666;

  @media (prefers-color-scheme: dark) {
    color: #888;
  }
}
.frame-count {
  font-weight: bold;
}

// tags
.tags {
  margin-top: 5px;
  margin-bottom: 10px;

  .badge {
    margin: 0 2px;
  }
}

// card buttons
.btn-secondary {
  margin-left: 5px;
}

// "more info" modal dialog
.modal {
  text-align: left;
}
.show {
  display: block;
}
.author-notes {
  white-space: pre-wrap;
}
pre {
  background: var(--bs-info-bg-subtle);
  border: solid var(--bs-info-border-subtle) 1px;
  padding: 4px;
  margin: 5px 0 25px 0;
  white-space: pre;
}
.modal div:not(:first-child) > h3 {
  margin-top: 20px;
}

// custom close button
.close {
  color: black;
  text-decoration: none;
  font-size: 20pt;
  padding: 0 10px;

  @media (prefers-color-scheme: dark) {
    color: white;
  }

  &:hover {
    color: #888;
  }
}
</style>
