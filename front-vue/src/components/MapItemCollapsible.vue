<template>
  <div class="collapsible">
    <div class="line">
      <div
        v-on:click="expandOrCollapse"
        :class="
          'tree-item ' +
          (item.children.length !== 0 ? 'clickable' : '') +
          ' ' +
          (highlighted ? 'highlight' : '')
        "
      >
        <span class="arrow" v-if="item.children.length === 0">–</span>
        <span v-else>
          <img
            :class="'arrow pointer dark' + (expanded ? '' : ' collapsed')"
            src="../assets/img/arrow-white.svg"
          />
          <img
            :class="'arrow pointer light' + (expanded ? '' : ' collapsed')"
            src="../assets/img/arrow-black.svg"
          />
        </span>

        <span class="item-name">{{ item.name }}</span>
      </div>
    </div>
    <div class="attributes">
      <span
        class="attribute"
        v-bind:key="attribute.key"
        v-for="(attribute, index) in orderedAttributes"
      >
        {{ attribute.key }}:
        <span class="att-value">
          <pre
            v-if="
              ['solids', 'bg'].includes(item.name) &&
              attribute.key === 'innerText'
            "
            >{{ attribute.value }}</pre
          >
          <span v-else>{{ attribute.value }}</span>
        </span>
        <span v-if="index != orderedAttributes.length - 1">, </span>
      </span>
    </div>

    <div v-if="expanded" class="child">
      <MapItemCollapsible
        v-bind:key="child"
        v-for="child in item.children"
        :item="child"
        :parent="item"
        :highlight="highlight"
      />
    </div>
  </div>
</template>

<script>
const isHighlighted = function (item, parent, highlight) {
  if (["decal", "parallax"].includes(item.name)) {
    if (
      item.attributes.texture !== undefined &&
      item.attributes.texture.toLowerCase().indexOf(highlight) >= 0
    ) {
      return true;
    }
  } else if (
    parent !== null &&
    ["entities", "triggers", "Foregrounds", "Backgrounds"].includes(parent.name)
  ) {
    if (item.name.toLowerCase().indexOf(highlight) >= 0) {
      return true;
    }
  }

  for (const child of item.children) {
    if (isHighlighted(child, item, highlight)) {
      return true;
    }
  }

  return false;
};

export default {
  props: ["item", "highlight", "parent"],
  data: () => ({
    expanded: false,
  }),
  methods: {
    expandOrCollapse: function () {
      this.expanded = !this.expanded;
    },
  },
  computed: {
    orderedAttributes() {
      const specialKeys = ["name", "x", "y", "width", "height", "id"];
      const result = [];
      const specials = [];
      for (const [key, value] of Object.entries(this.item.attributes)) {
        if (specialKeys.includes(key)) {
          specials.push({ key, value });
        } else {
          result.push({ key, value });
        }
      }
      specials.sort(
        (a, b) => specialKeys.indexOf(a.key) - specialKeys.indexOf(b.key)
      );
      result.sort((a, b) =>
        a.key.toLowerCase().localeCompare(b.key.toLowerCase())
      );

      specials.push(...result);
      return specials;
    },
    highlighted() {
      if (this.highlight === "") return false;
      return isHighlighted(
        this.item,
        this.parent,
        this.highlight.toLowerCase()
      );
    },
  },
};
</script>

<style lang="scss" scoped>
.child {
  margin-left: 20px;
}

.clickable {
  cursor: pointer;
  user-select: none;
}

.tree-item {
  margin-top: 2px;
  margin-bottom: 2px;
}

.highlight {
  padding: 3px;
  background-color: yellow;
  border: solid black 3px;
  border-radius: 10px;
}

@media (prefers-color-scheme: dark) {
  .highlight {
    border: solid lightgray 3px;
    background-color: #660;
  }
}

.arrow {
  display: inline-block;
  width: 1.25rem;
  height: 1.25rem;
  margin-right: 5px;

  &.dark {
    display: none;
  }

  @media (prefers-color-scheme: dark) {
    &.dark {
      display: inline-block;
    }
    &.light {
      display: none;
    }
  }

  &:not(.pointer) {
    text-align: center;
  }

  &.pointer {
    margin-bottom: 2px; // I don't get vertical alignment in CSS and gave up on trying
    transition: transform 0.2s ease-in-out;

    @media (prefers-reduced-motion: reduce) {
      transition: none;
    }

    &.collapsed {
      transform: rotate(-90deg);
    }
  }
}

@media (prefers-color-scheme: dark) {
  .arrow.pointer {
    background: no-repeat center url("../assets/img/arrow-white.svg");
  }
}

.attributes {
  padding-left: 26px;
}

.attribute {
  color: gray;
  font-style: italic;
  font-size: 10pt;
  font-weight: bold;
}
.att-value {
  font-weight: normal;
}
</style>
