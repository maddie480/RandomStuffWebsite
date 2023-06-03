<template>
  <div>
    <h1>❌ Not Found</h1>
    <a href="/">⬅ Back to Home Page</a>
  </div>
</template>

<script>
const vue = {
  name: "route-not-found",

  // hide navbar on this 404 page
  mounted: function () {
    document.getElementById("nav").style.display = "none";
  },
  unmounted: function () {
    document.getElementById("nav").style.display = "block";
  },
};

export default vue;
</script>

<style lang="scss" scoped>
h1 {
  margin-top: 12px;
  margin-bottom: 22px;
  font-size: 32px;
  font-weight: bold;
}
</style>
