<template>
  <div>
    <nav
      class="navbar navbar-expand navbar-light bg-light border-bottom shadow-sm"
      id="nav"
    >
      <div class="container-fluid">
        <h5 class="navbar-brand m-0">max480's Random Stuff</h5>

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
                <a class="dropdown-item" href="/celeste/custom-entity-catalog"
                  >Custom Entity Catalog</a
                >
              </li>
              <li>
                <a class="dropdown-item" href="/celeste/everest-yaml-validator"
                  >everest.yaml validator</a
                >
              </li>
              <li>
                <a class="dropdown-item" href="/celeste/font-generator"
                  >Font Generator</a
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
                  >GameBanana</a
                >
              </li>
              <li>
                <a
                  class="dropdown-item"
                  href="https://github.com/max4805"
                  target="_blank"
                  >GitHub</a
                >
              </li>
            </ul>
          </li>
        </ul>
      </div>
    </nav>

    <div id="app" class="container">
      <router-view />
    </div>
  </div>
</template>

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
  .navbar .navbar-brand {
    color: #fff;
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
}
</style>
