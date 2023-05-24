<template>
  <div :class="'month-' + (new Date().getMonth() + 1)">
    <nav
      class="navbar navbar-expand navbar-light bg-light border-bottom shadow-sm"
      id="nav"
    >
      <div class="container-fluid">
        <h5 class="navbar-brand m-0">
          <a href="/"
            ><img src="/img/maddie-avatar.png" /> Maddie's Random Stuff</a
          >
        </h5>

        <ul class="navbar-nav">
          <li class="nav-item dropdown">
            <a
              :class="
                'nav-link dropdown-toggle ' +
                (this.$route.path.startsWith('/celeste') ? 'active' : '')
              "
              href="#"
              id="celesteDropdown"
              role="button"
              data-bs-toggle="dropdown"
              data-bs-target="#celesteDropdown"
              aria-expanded="false"
              >Celeste</a
            >
            <ul
              class="dropdown-menu dropdown-menu-sm-end"
              aria-labelledby="celesteDropdown"
            >
              <li>
                <a
                  :class="
                    'dropdown-item ' +
                    (this.$route.path === '/celeste/banana-mirror-browser'
                      ? 'active'
                      : '')
                  "
                  href="/celeste/banana-mirror-browser"
                  >Banana Mirror Browser</a
                >
              </li>
              <li>
                <a
                  class="dropdown-item"
                  href="/celeste/news-network-subscription"
                  >#celeste_news_network Subscription</a
                >
              </li>
              <li>
                <a class="dropdown-item" href="/celeste/collab-contest-list"
                  >Collab & Contest List</a
                >
              </li>
              <li>
                <a class="dropdown-item" href="/celeste/custom-entity-catalog"
                  >Custom Entity Catalog</a
                >
              </li>
              <li>
                <a class="dropdown-item" href="/celeste/direct-link-service"
                  >Direct Link service</a
                >
              </li>
              <li>
                <a class="dropdown-item" href="/celeste/everest-yaml-validator"
                  >everest.yaml validator</a
                >
              </li>
              <li>
                <a
                  :class="
                    'dropdown-item ' +
                    (this.$route.path === '/celeste/file-searcher'
                      ? 'active'
                      : '')
                  "
                  href="/celeste/file-searcher"
                  >File Searcher</a
                >
              </li>
              <li>
                <a class="dropdown-item" href="/celeste/font-generator"
                  >Font Generator</a
                >
              </li>
              <li>
                <a
                  :class="
                    'dropdown-item ' +
                    (this.$route.path === '/celeste/map-tree-viewer'
                      ? 'active'
                      : '')
                  "
                  href="/celeste/map-tree-viewer"
                  >Map Tree Viewer</a
                >
              </li>
              <li>
                <a class="dropdown-item" href="/celeste/mod-structure-verifier"
                  >Mod Structure Verifier</a
                >
              </li>
              <li>
                <a class="dropdown-item" href="/celeste/update-checker-status"
                  >Update Checker status</a
                >
              </li>
              <li>
                <a
                  :class="
                    'dropdown-item ' +
                    (this.$route.path === '/celeste/wipe-converter'
                      ? 'active'
                      : '')
                  "
                  href="/celeste/wipe-converter"
                  >Wipe Converter</a
                >
              </li>
            </ul>
          </li>
          <li class="nav-item">
            <a class="nav-link" href="/discord-bots">Discord Bots</a>
          </li>
          <li class="nav-item dropdown">
            <a
              class="nav-link dropdown-toggle"
              href="#"
              id="linksDropdown"
              role="button"
              data-bs-toggle="dropdown"
              aria-expanded="false"
              >Links</a
            >
            <ul
              class="dropdown-menu dropdown-menu-end"
              aria-labelledby="linksDropdown"
            >
              <li>
                <a
                  class="dropdown-item"
                  href="https://gamebanana.com/members/1698143"
                  target="_blank"
                  >&#x1F34C;&nbsp;&nbsp;GameBanana &#x2013; Celeste Mods</a
                >
              </li>
              <li>
                <a
                  class="dropdown-item"
                  href="https://github.com/maddie480"
                  target="_blank"
                  >&#x1F4BB;&nbsp;&nbsp;GitHub &#x2013; Source Code</a
                >
              </li>
            </ul>
          </li>
        </ul>
      </div>
    </nav>

    <div id="app" class="container">
      <div class="alert alert-warning ovh-warning" v-if="isLegacyOvhWebsite">
        <b>The max480.ovh domain will expire on January 31, 2024.</b>
        Before then, update your bookmarks / links / scripts to point to
        <a href="https://maddie480.ovh/">maddie480.ovh</a> instead! Thanks
        &#x1F604;
      </div>
      <router-view />
    </div>
  </div>
</template>

<script>
const vue = {
  name: "app",
  computed: {
    isLegacyOvhWebsite: () => document.location.host === "max480.ovh",
  },
};

export default vue;
</script>

<style lang="scss">
@import "../node_modules/bootstrap/scss/bootstrap.scss";

#app {
  text-align: center;
}

.navbar {
  margin-bottom: 20px;
}

.dropdown-item.active {
  color: #fff;
  text-decoration: none;
  background-color: #0d6efd;
}

.form-group {
  margin-bottom: 16px;
}

// navbar modifications when the screen is not wide enough
@media (max-width: 575px) {
  .container-fluid {
    display: block !important;
    text-align: center;
  }
  .navbar-nav {
    display: block;
  }
  .nav-item {
    display: inline-block;
  }
}

.navbar,
.dropdown-menu {
  background: url("/assets/img/white.png"),
    url("/assets/img/bg-light.png") top / cover;
}

.month-12 .navbar,
.month-1 .navbar,
.month-2 .navbar,
.month-11 .dropdown-menu,
.month-12 .dropdown-menu,
.month-1 .dropdown-menu,
.month-2 .dropdown-menu {
  background: url("/assets/img/white.png"),
    url("/assets/img/snow-front.png") center / cover,
    url("/assets/img/bg-light-alt.png") center / cover;
}

.navbar .navbar-brand a {
  color: rgb(33, 37, 41);
  text-decoration: none;
}

.navbar-brand img {
  border-radius: 50%;
  height: 50px;
  margin-right: 7px;
  margin-top: -14px;
  margin-bottom: -5px;
}

.month-6 .navbar-brand a::before {
  border-radius: 50%;
  height: 56px;
  width: 56px;
  background: linear-gradient(
    to bottom,
    #5bcefa 0% 20%,
    #f5a9b8 20% 40%,
    #fff 40% 60%,
    #f5a9b8 60% 80%,
    #5bcefa 80% 100%
  );
  content: "";
  display: inline-block;
  margin-right: -53px;
  margin-bottom: -16px;
  margin-top: -20px;
}

.month-6 .navbar-brand img {
  background-color: #fcf6e0;
}

@font-face {
  font-family: "Gontserrat";
  src: url("/assets/fonts/Gontserrat-Light.ttf");
}
@font-face {
  font-family: "Gontserrat";
  font-weight: bold;
  src: url("/assets/fonts/Gontserrat-Bold.ttf");
}
@font-face {
  font-family: "Gontserrat";
  font-style: italic;
  src: url("/assets/fonts/Gontserrat-LightItalic.ttf");
}
@font-face {
  font-family: "Gontserrat";
  font-weight: bold;
  font-style: italic;
  src: url("/assets/fonts/Gontserrat-BoldItalic.ttf");
}
@font-face {
  font-family: "Hey August";
  src: url("/assets/fonts/HeyAugust.otf");
}

.navbar .navbar-brand a {
  color: rgb(33, 37, 41);
  text-decoration: none;
  font-family: "Hey August", sans-serif;
  font-size: 30px;
}
body {
  font-family: "Gontserrat", sans-serif;
}
h1,
h2,
h3 {
  font-family: "Hey August", sans-serif;
}

// dark theme colors
@media (prefers-color-scheme: dark) {
  // global app colors
  body {
    background-color: black;
  }
  #app {
    color: #dedad6;
  }

  // navbar colors
  .navbar.bg-light {
    background-color: rgb(33, 37, 41) !important;
    border-bottom: 1px solid #8a8a8a !important;
  }
  .navbar .navbar-brand,
  .navbar .navbar-brand a {
    color: #fff;
  }
  .navbar,
  .dropdown-menu {
    background: url("/assets/img/black.png"),
      url("/assets/img/bg-dark-l4.png") center / cover,
      url("/assets/img/bg-dark-l3.png") center / cover,
      url("/assets/img/bg-dark-l1.png") center / cover;
  }
  .month-6 .navbar-brand img {
    background-color: #361738;
  }
  .month-12 .navbar,
  .month-1 .navbar,
  .month-2 .navbar,
  .month-12 .dropdown-menu,
  .month-1 .dropdown-menu,
  .month-2 .dropdown-menu {
    background: url("/assets/img/black.png"), url("/assets/img/black.png"),
      url("/assets/img/snow-front.png") center / cover,
      url("/assets/img/bg-dark-alt.png") top / cover;
  }
  .navbar .navbar-nav .nav-link {
    color: rgba(255, 255, 255, 0.55);
  }
  .navbar .navbar-nav .nav-link:focus,
  .navbar .navbar-nav .nav-link:hover {
    color: rgba(255, 255, 255, 0.75);
  }
  .navbar .navbar-nav .nav-link.active {
    color: #fff;
  }
  .navbar-light .navbar-brand:hover {
    color: #fff;
  }
  .dropdown-menu {
    color: #dee2e6;
    background-color: #343a40;
    border-color: rgba(0, 0, 0, 0.15);
  }
  .dropdown-menu .dropdown-item {
    color: #dee2e6;
  }
  .dropdown-menu .dropdown-item:focus:not(.active),
  .dropdown-menu .dropdown-item:hover:not(.active) {
    color: #fff;
    background-color: rgba(255, 255, 255, 0.15);
  }

  // button colors
  .btn-primary {
    background-color: #004dbf;
    border-color: #125cc8;
  }
  .btn-secondary {
    background-color: #484e53;
    border-color: #555d64;
  }
  .btn-outline-secondary {
    color: #909ca6;
    border-color: #909ca6;
  }

  // table colors
  .table {
    border-color: #75787b;
  }
  .table-striped > tbody > tr:nth-of-type(2n + 1) {
    background-color: #222;
  }

  // form control colors
  .form-control {
    background-color: rgba(0, 0, 0, 0);
    border-color: #83888c;
    color: #dedad6;

    &::placeholder {
      color: #8e8e8e;
    }
  }
  .form-control:focus {
    background-color: rgba(0, 0, 0, 0);
    color: #dedad6;
    border-color: #125cc8;
  }

  // better contrast for inline code
  code {
    color: #ff5c99;
  }

  // better contrast for links
  @media (prefers-color-scheme: dark) {
    a {
      color: #47a0ff;
    }
  }

  // max480.ovh warning colors
  .ovh-warning {
    &.alert-warning {
      color: #fff;
      border-color: #b97b00;
      background-color: #8e5f00;
    }
    a {
      color: #ddd;
      text-decoration: underline;
    }
  }
}

.ovh-warning {
  text-align: left;
}
</style>
