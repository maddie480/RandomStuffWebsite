<template>
  <div class="mod-container">
    <div
      class="bg"
      v-if="mod.MirroredScreenshots.length >= 2"
      :style="
        'background: url(\'' +
        mod.MirroredScreenshots[1] +
        '\') center / cover;'
      "
    />
    <div class="darken" v-if="mod.MirroredScreenshots.length >= 2" />

    <div
      class="contents"
      :style="mod.MirroredScreenshots.length >= 2 ? 'margin-top: -290px' : ''"
    >
      <div>
        <a :href="mod.PageURL" rel="noopener" target="_blank" class="gb-link">{{
          mod.Name
        }}</a>
      </div>
      <div class="secondary">{{ mod.Author }}</div>
      <table>
        <tr style="mod-screenshot-and-meta">
          <td>
            <div class="screenshot">
              <img :src="mod.MirroredScreenshots[0]" />
            </div>
          </td>
          <td>
            <div class="secondary metadata">
              {{
                new Date(mod.CreatedDate * 1000).toLocaleDateString(undefined, {
                  hour12: false,
                  year: "numeric",
                  month: "2-digit",
                  day: "2-digit",
                  hour: "2-digit",
                  minute: "2-digit",
                  second: "2-digit",
                })
              }}<br />
              {{ mod.Views }} view{{ mod.Views === 1 ? "" : "s" }} -
              {{ mod.Likes }} like{{ mod.Likes === 1 ? "" : "s" }}<br />
              {{ mod.Downloads }} download{{ mod.Downloads === 1 ? "" : "s" }}
            </div>
          </td>
        </tr>
      </table>
      <div>
        {{ mod.Description }}
      </div>

      <img
        v-if="getPictureFor(mod.Featured)"
        class="featured"
        :src="getPictureFor(mod.Featured)"
        :title="getLabelFor(mod.Featured)"
      />
      <div class="actions">
        <button class="btn btn-secondary" v-on:click="openDescription">
          Description
        </button>
        <button class="btn btn-success" v-on:click="openDownloads">
          Download
        </button>
      </div>
    </div>

    <div class="modal fade show" tabindex="-1" v-if="descriptionShown">
      <div class="modal-dialog modal-lg">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">{{ mod.Name }}</h5>
            <button
              type="button"
              class="btn btn-link close"
              aria-label="Close"
              v-on:click="closeDescription"
            >
              ×
            </button>
          </div>
          <div class="modal-body">
            <p class="gamebanana-description-html" v-html="mod.Text"></p>
          </div>
        </div>
      </div>
    </div>

    <div class="modal fade show" tabindex="-1" v-if="downloadsShown">
      <div class="modal-dialog modal-lg">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">Download {{ mod.Name }}</h5>
            <button
              type="button"
              class="btn btn-link close"
              aria-label="Close"
              v-on:click="closeDownloads"
            >
              ×
            </button>
          </div>
          <div class="modal-body">
            <table
              class="table table-striped"
              v-if="getMirroredFiles(mod.Files).length !== 0"
            >
              <tbody>
                <tr
                  v-bind:key="file.URL"
                  v-for="file in this.getMirroredFiles(mod.Files)"
                >
                  <td class="first">
                    <div>
                      <div>
                        <span class="name-full">{{ file.Name }}</span>
                        <span class="name-truncated">{{
                          file.Name.length > 10
                            ? file.Name.substring(0, 15) + "..."
                            : file.Name
                        }}</span>
                        <span
                          class="secondary smaller large-only"
                          v-if="'EverestYamlId' in file"
                        >
                          ({{ file.EverestYamlId }})</span
                        >
                      </div>
                      <div class="smaller">
                        {{
                          file.Description +
                          (file.Description !== "" ? " - " : "")
                        }}
                        {{
                          file.Size > 1048576
                            ? (file.Size / 1048576).toFixed(2) + " MB"
                            : file.Size > 1024
                            ? (file.Size / 1024).toFixed(2) + " KB"
                            : file.Size + " B"
                        }}
                        <span class="large-only"
                          >-
                          {{
                            new Date(
                              file.CreatedDate * 1000
                            ).toLocaleDateString(undefined, {
                              hour12: false,
                              year: "numeric",
                              month: "2-digit",
                              day: "2-digit",
                              hour: "2-digit",
                              minute: "2-digit",
                              second: "2-digit",
                            })
                          }}</span
                        >
                      </div>
                    </div>
                  </td>
                  <td class="second" v-if="getMirrorLink(file) !== null">
                    <a
                      class="btn btn-primary"
                      :href="'everest:' + getMirrorLink(file)"
                    >
                      1-click install
                    </a>
                    <a class="btn btn-secondary" :href="getMirrorLink(file)"
                      >Download</a
                    >
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="getMirroredFiles(mod.Files).length === 0">
              <div class="not-mirrored text-danger">
                ❌ This mod is not mirrored
              </div>
              <div class="not-mirrored-help">
                Only mods with a valid <code>everest.yaml</code> file are
                available on this mirror.<br />Try downloading it
                <a :href="mod.PageURL" target="_blank"
                  >from GameBanana directly</a
                >
                or switching to another mirror.
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div
      class="modal-backdrop fade show"
      v-if="descriptionShown || downloadsShown"
    />
  </div>
</template>

<script>
export default {
  props: ["mod", "mirror"],
  data: () => ({
    descriptionShown: false,
    downloadsShown: false,
  }),
  methods: {
    openDescription() {
      this.descriptionShown = true;
    },
    closeDescription() {
      this.descriptionShown = false;
    },
    openDownloads() {
      this.downloadsShown = true;
    },
    closeDownloads() {
      this.downloadsShown = false;
    },
    getLabelFor(featuredCategory) {
      switch (featuredCategory) {
        case "today":
          return "Best of today";
        case "week":
          return "Best of this week";
        case "month":
          return "Best of this month";
        case "3month":
          return "Best of 3 months";
        case "6month":
          return "Best of 6 months";
        case "year":
          return "Best of this year";
        case "alltime":
          return "Best of all time";
        default:
          return "";
      }
    },
    getPictureFor(featuredCategory) {
      switch (featuredCategory) {
        case "today":
          return "";
        case "week":
          return "/img/smallstar.png";
        case "month":
          return "/img/thirdplace.png";
        case "3month":
          return "/img/secondplace.png";
        case "6month":
          return "/img/firstplace.png";
        case "year":
          return "/img/trophy.png";
        case "alltime":
          return "/img/ripe.png";
        default:
          return "";
      }
    },
    getMirrorLink(file) {
      if (this.mirror === "wegfan") {
        return `https://celeste.weg.fan/api/v2/download/gamebanana-files/${file.GameBananaFileId}`;
      }

      if ("EverestYamlId" in file) {
        return `https://celestemodupdater.0x0a.de/banana-mirror/${file.GameBananaFileId}.zip`;
      }

      return null;
    },
    getMirroredFiles(files) {
      return files.filter((file) => this.getMirrorLink(file) !== null);
    },
  },
};
</script>

<style lang="scss" scoped>
// frame that contains the mod info
.mod-container {
  text-align: left;
  border: solid #aaa 1px;
  border-radius: 5px;
  height: 300px;
  margin: 12px 0 12px 0;

  overflow: hidden;
  position: relative;

  @media (prefers-color-scheme: dark) {
    border: solid #777 1px;
  }
}

// title that links to the GameBanana page
.gb-link {
  color: rgb(33, 37, 41);
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }

  @media (prefers-color-scheme: dark) {
    color: #dedad6;
  }
}

.show {
  display: block;
}

// blurry background
.bg {
  height: 300px;
  width: 100%;

  filter: blur(8px);
  -webkit-filter: blur(8px);

  position: relative;
  z-index: -2;
}

// extra layer to darken / brighten the blurry background
.darken {
  background-color: white;
  opacity: 0.85;
  height: 300px;
  width: 100%;
  margin-top: -300px;

  position: relative;
  z-index: -1;

  @media (prefers-color-scheme: dark) {
    background-color: black;
    opacity: 0.7;
  }
}

// actual contents of the frame
.contents {
  margin: 10px;
}

.screenshot {
  display: inline-block;

  img {
    max-width: 120px;
    max-height: 120px;
    width: auto;
    height: auto;
  }
}

// all "secondary" text that is greyed out a bit
.secondary {
  color: #666;

  @media (prefers-color-scheme: dark) {
    color: #888;
  }
}

// smaller text in italic used in the file list
.smaller {
  font-size: 11pt;
  font-style: italic;

  @media (max-width: 800px) {
    font-size: 10pt;
  }
}

// elements that should only be visible on the largest configuration
@media (max-width: 990px) {
  .large-only {
    display: none;
  }
}

// switch to truncated file names if the device width is < 450px
.name-truncated {
  display: none;
}
@media (max-width: 450px) {
  .name-truncated {
    display: inline;
  }
  .name-full {
    display: none;
  }
}

// date, view count, like count, download count
.metadata {
  display: table-cell;
  height: 120px;
  margin-left: 15px;
  vertical-align: middle;
  padding-left: 10px;
}

// "featured" star positioning
.featured {
  position: absolute;
  bottom: 14px;
  left: 14px;
}

// description and download buttons
.actions {
  position: absolute;
  bottom: 15px;
  right: 15px;

  button {
    margin-left: 10px;
  }
}

// dark theme for modal dialogs
@media (prefers-color-scheme: dark) {
  .modal-content {
    background-color: black;
    border-color: #777;
  }
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

// style for the download dialog: label
.first {
  text-align: left;
  vertical-align: middle;
  font-size: 14pt;
  color: rgb(33, 37, 41);

  @media (max-width: 800px) {
    font-size: 12pt;
  }

  @media (prefers-color-scheme: dark) {
    color: #dedad6;
  }
}

.not-mirrored {
  font-size: 16pt;
  text-align: center;
}
.not-mirrored-help {
  text-align: center;
  margin-top: 10px;
}

// make Bootstrap not kill the download list in dark theme
.table-striped > tbody > tr:nth-of-type(2n + 1) > td.first {
  @media (prefers-color-scheme: dark) {
    color: #dedad6;
  }
}

// style for the download dialog: buttons column
.second {
  text-align: right;
  vertical-align: middle;

  .btn {
    margin: 2px;
  }
}
</style>

<style lang="scss">
// **non-scoped** CSS for GameBanana formatting
// (HTML from GameBanana doesn't have the proper attributes for scoped styles)
.gamebanana-description-html {
  .GreenColor {
    color: #3caa3a;
    @media (prefers-color-scheme: dark) {
      color: rgb(110, 225, 108);
    }
  }
  .RedColor {
    color: #e62222;
    @media (prefers-color-scheme: dark) {
      color: rgb(255, 78, 78);
    }
  }
  .Spoiler {
    background: rgba(0, 0, 0, 0.25);
    position: relative;
    cursor: help;
    color: transparent;

    @media (prefers-color-scheme: dark) {
      background: rgba(255, 255, 255, 0.25);
    }

    &:hover {
      background: none;
      color: rgb(44, 62, 80);

      @media (prefers-color-scheme: dark) {
        color: rgb(222, 218, 214);
      }
    }

    & > * {
      visibility: hidden;

      &:hover {
        visibility: visible;
      }
    }
  }

  // better contrast for links in dark mode
  @media (prefers-color-scheme: dark) {
    a {
      color: #47a0ff;
    }
  }
}
</style>
