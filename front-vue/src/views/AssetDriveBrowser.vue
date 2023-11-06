<template>
  <div>
    <h1>Asset Drive Browser</h1>

    <div class="intro first">
      This page allows you to browse the
      <b
        ><a
          href="https://drive.google.com/drive/folders/13A0feEXS3kUHb_Q4K2w4xP8DEuBlgTip"
          target="_blank"
          >Celeste Community Asset Drive</a
        ></b
      >, with added features such as filtering, search and tags.
    </div>
    <div class="intro" v-if="lastUpdate !== null">
      The list is updated <b>once an hour</b> from Google Drive. The last update
      happened on <b>{{ lastUpdate }}</b
      >.
    </div>

    <div class="alert alert-info">
      <b
        >Check out
        <a
          href="https://drive.google.com/file/d/1e4bwS_Nvl0654yXu7SV9vBXtT5LBxkTJ/view"
          target="_blank"
          >the README</a
        ></b
      >
      for more information about how to use the assets, how to add your own to
      the drive, etc.
      <br />
      Also make sure to <b>click the "More info" button</b> for assets that have
      one! It may contain extra instructions on how to use the assets, or may
      impose conditions on their usage (such as crediting, notifying before
      usage or modification...).
    </div>

    <div class="categories">
      <button
        :class="
          'btn ' + (currentCategory === 'decals' ? 'btn-primary' : 'btn-link')
        "
        v-on:click="currentCategory = 'decals'"
      >
        Decals
      </button>
      <button
        :class="
          'btn ' +
          (currentCategory === 'stylegrounds' ? 'btn-primary' : 'btn-link')
        "
        v-on:click="currentCategory = 'stylegrounds'"
      >
        Stylegrounds
      </button>
      <button
        :class="
          'btn ' +
          (currentCategory === 'bgtilesets' ? 'btn-primary' : 'btn-link')
        "
        v-on:click="currentCategory = 'bgtilesets'"
      >
        Background Tilesets
      </button>
      <button
        :class="
          'btn ' +
          (currentCategory === 'fgtilesets' ? 'btn-primary' : 'btn-link')
        "
        v-on:click="currentCategory = 'fgtilesets'"
      >
        Foreground Tilesets
      </button>
      <button
        :class="
          'btn ' + (currentCategory === 'misc' ? 'btn-primary' : 'btn-link')
        "
        v-on:click="currentCategory = 'misc'"
      >
        Misc
      </button>
    </div>

    <AssetDriveCategory
      class="category-listing"
      category="decals"
      category-display-name="decal"
      v-if="currentCategory === 'decals'"
    />
    <AssetDriveCategory
      class="category-listing"
      category="stylegrounds"
      category-display-name="styleground"
      v-else-if="currentCategory === 'stylegrounds'"
    />
    <AssetDriveCategory
      class="category-listing"
      category="bgtilesets"
      category-display-name="tileset"
      v-else-if="currentCategory === 'bgtilesets'"
    />
    <AssetDriveCategory
      class="category-listing"
      category="fgtilesets"
      category-display-name="tileset"
      v-else-if="currentCategory === 'fgtilesets'"
    />
    <AssetDriveCategory
      class="category-listing"
      category="misc"
      category-display-name="asset"
      v-else-if="currentCategory === 'misc'"
    />
    <div class="category-empty" v-else>
      Select a category above to see the assets.
    </div>
  </div>
</template>

<script>
import axios from "axios";
import config from "../config";
import AssetDriveCategory from "../components/AssetDriveCategory.vue";

export default {
  components: { AssetDriveCategory },
  name: "asset-drive-browser",
  data: () => ({
    currentCategory: null,
    lastUpdate: null,
  }),
  mounted: async function () {
    const lastUpdateISO = (
      await axios.get(`${config.backendUrl}/celeste/asset-drive/last-updated`)
    ).data;

    this.lastUpdate = new Date(lastUpdateISO).toLocaleDateString("en-US", {
      hour12: true,
      year: "numeric",
      month: "long",
      day: "numeric",
      hour: "numeric",
      minute: "2-digit",
      second: "2-digit",
    });
  },
};
</script>

<style scoped lang="scss">
.intro {
  text-align: left;
  margin: 10px 5px;

  &.first {
    margin-top: 20px;
  }
}

.categories {
  margin: 10px 0;
}
.category-empty {
  font-weight: bold;
  margin-top: 20px;
}

.alert-info {
  text-align: left;
}
</style>
