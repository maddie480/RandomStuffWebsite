<template>
  <div>
    <asset-list-collapsible categoryId="tilesets" categoryName="Tilesets" />
    <asset-list-collapsible
      categoryId="stylegrounds"
      categoryName="Stylegrounds"
    />
    <asset-list-collapsible categoryId="decals" categoryName="Decals" />
    <asset-list-collapsible categoryId="misc" categoryName="Misc" />
  </div>
</template>

<script>
import AssetListCollapsible from "../components/AssetListCollapsible.vue";

export default {
  components: { AssetListCollapsible },
  name: "asset-browser",
};
</script>
