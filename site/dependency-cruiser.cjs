module.exports = {
  forbidden: [
    {
      name: "no-circular",
      severity: "error",
      from: {},
      to: { circular: true },
    },
    {
      name: "api-stays-framework-free",
      severity: "error",
      comment:
        "Feed and embed contracts should stay reusable outside React presentation.",
      from: { path: "^src/api/" },
      to: { path: "^src/(?:components|pages|theme)/" },
    },
    {
      name: "ui-components-stay-foundational",
      severity: "error",
      comment: "Low-level shadcn primitives should not depend on app code.",
      from: { path: "^src/components/ui/" },
      to: { path: "^src/(?:api|components/app|pages|theme)/" },
    },
    {
      name: "registry-components-only-through-app",
      severity: "error",
      comment:
        "Pages and other code use shadcn primitives through components/app wrappers.",
      from: { path: "^src/", pathNot: "^src/components/(?:app|ui)/" },
      to: { path: "^src/components/ui/" },
    },
    {
      name: "custom-components-stay-below-app",
      severity: "error",
      comment:
        "Custom components such as provider glyphs sit outside the wrapper layer and below pages.",
      from: { path: "^src/components/custom/" },
      to: { path: "^src/(?:components/(?:app|ui)|pages|theme)/" },
    },
    {
      name: "app-components-do-not-reach-pages",
      severity: "error",
      comment:
        "Reusable app wrappers should not depend on page composition details.",
      from: { path: "^src/components/app/" },
      to: { path: "^src/pages/" },
    },
    {
      name: "theme-does-not-reach-pages",
      severity: "error",
      comment: "Theme plumbing should remain below page composition.",
      from: { path: "^src/theme/" },
      to: { path: "^src/pages/" },
    },
  ],
  options: {
    doNotFollow: {
      path: "node_modules",
    },
    exclude: {
      path: "dist|node_modules|[.]test[.]tsx?$",
    },
    tsConfig: {
      fileName: "tsconfig.depcruise.json",
    },
  },
};
