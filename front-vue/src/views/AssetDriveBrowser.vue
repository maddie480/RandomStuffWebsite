<template>
  <div>
    <h1>Asset Drive Browser</h1>

    <div class="alert alert-info">
      <b>Make sure to check the "More Info" button for assets that have it!</b>
      It may contain instructions on how to use the assets, or may impose
      conditions on their usage (such as crediting, notifying before usage or
      modification...).
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
import AssetDriveCategory from "../components/AssetDriveCategory.vue";

export default {
  components: { AssetDriveCategory },
  name: "asset-drive-browser",
  data: () => ({
    currentCategory: null,
  }),
};
</script>

<style scoped lang="scss">
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
