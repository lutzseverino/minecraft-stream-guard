# Keep StreamGuard and its site in one repository

The plugin and its live stream wall are one StreamGuard product. On 2026-11-08
the owner chose to keep minecraft-stream-guard and import
minecraft-stream-guard-site with full unsquashed history. Separate repositories
made coordinated development and adoption harder; a single repository for all
Minecraft products would instead join StreamGuard to a distinct setup product.

The destination has plugin/ and site/ components, shared contribution guidance
and one root standards/factory adoption. Both toolchains and component READMEs
remain. Plugin releases use independent plugin-v… tags; site builds and
deployments do not require a plugin release. Co-location does not replace the
HTTP feed contract or require simultaneous deployments.

The import merge preserves the site's ancestry rather than squash it. Open
issues transfer and affected briefs are revalidated. The old site repository is
archived with a destination pointer after verification. Existing plugin
releases and their historical tags remain accessible. Serving the static wall
and proving its public feed still belong to the hosting/live acceptance work;
source consolidation grants no deployment or host operation.

This records the chosen architecture; it does not claim that migration or live
acceptance has happened.
