<template>
  <div class="col-xl-3 col-lg-4 col-md-6 col-xs-12 base">
    <div class="card">
      <div class="card-body">
        <div class="image-container">
          <img :src="imagePath" loading="lazy" />
        </div>
        <h5 class="card-title">{{ name }}</h5>
        <a
          class="btn btn-primary"
          target="_blank"
          :href="'/vanilla-graphics-dump/' + path"
          >Open</a
        >
        <button class="btn btn-secondary" v-on:click="copyPath">
          Copy path
        </button>
      </div>

      <div
        class="toast fade hide"
        style="position: fixed; bottom: 10px; right: 10px"
        :id="'success-' + path.replace('/', '-')"
      >
        <div class="toast-body bg-success">
          Path "{{ path }}" copied to the clipboard!
        </div>
      </div>
      <div
        class="toast fade hide"
        style="position: fixed; bottom: 10px; right: 10px"
        :id="'failure-' + path.replace('/', '-')"
      >
        <div class="toast-body bg-danger">
          Failed to copy path "{{ path }}" to the clipboard!
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import config from "../config";

export default {
  props: ["name", "path"],
  data: () => ({
    expanded: false,
  }),
  methods: {
    async copyPath() {
      const slugifiedPath = this.path.replace("/", "-");

      navigator.clipboard.writeText(this.path).then(
        function () {
          // eslint-disable-next-line no-undef
          const toastBootstrap = bootstrap.Toast.getOrCreateInstance(
            document.getElementById("success-" + slugifiedPath),
          );
          toastBootstrap.show();
        },
        function () {
          // eslint-disable-next-line no-undef
          const toastBootstrap = bootstrap.Toast.getOrCreateInstance(
            document.getElementById("failure-" + slugifiedPath),
          );
          toastBootstrap.show();
        },
      );
    },
  },
  computed: {
    imagePath() {
      return config.backendUrl + "/vanilla-graphics-dump/" + this.path;
    },
  },
};
</script>

<style lang="scss" scoped>
.card {
  margin: 0 5px 10px 5px;
}
.base {
  margin: 0;
  padding: 0;
}

.btn-secondary {
  margin-left: 5px;
}
.image-container {
  text-align: center;
}

img {
  max-width: 100%;
}

.bg-success,
.bg-danger {
  color: #fff;
}

.toast {
  z-index: 100;
}

.card-title {
  margin: 10px 0;
}
</style>
