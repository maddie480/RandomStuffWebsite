import { createApp } from "vue";
import Vuex from "vuex";
import App from "./App.vue";
import router from "./router";

const app = createApp(App);
app.use(Vuex);
app.use(router);
app.mount("#app");
