import { nextTick } from "vue";
import { createRouter, createWebHistory } from "vue-router";

const routes = [
  {
    path: "/",
    name: "HomePage",
    component: () => import("../views/HomePage.vue"),
  },
  {
    path: "/celeste/banana-mirror-browser",
    name: "BananaMirrorBrowser",
    component: () => import("../views/BananaMirrorBrowser.vue"),
    meta: {
      title: "Banana Mirror Browser",
    },
  },
  {
    path: "/celeste/mapping-sprite-resources",
    name: "AssetBrowser",
    component: () => import("../views/AssetBrowser.vue"),
    meta: {
      title: "Celeste Mapping Sprite Resources",
    },
  },
  {
    path: "/celeste/wipe-converter",
    name: "WipeConverter",
    component: () => import("../views/WipeConverter.vue"),
    meta: {
      title: "Wipe Converter",
    },
  },
  {
    path: "/:pathMatch(.*)*",
    component: () => import("../views/RouteNotFound.vue"),
  },
];

const router = createRouter({
  history: createWebHistory(),
  base: process.env.BASE_URL,
  routes,
});

const DEFAULT_TITLE = "max480's Random Stuff";
router.afterEach((to) => {
  // Use next tick to handle router history correctly
  // see: https://github.com/vuejs/vue-router/issues/914#issuecomment-384477609
  nextTick(() => {
    document.title = to.meta.title || DEFAULT_TITLE;
  });
});

export default router;
