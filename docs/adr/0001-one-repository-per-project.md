# Keep StreamGuard and its site in one repository

The plugin and its live stream wall are one StreamGuard product. On 2026-11-08
the owner chose to keep minecraft-stream-guard and import
minecraft-stream-guard-site with full unsquashed history. Separate repositories
made coordinated development and adoption harder; a single repository for all
Minecraft products would instead join StreamGuard to a distinct setup product.
The owner confirmed the consolidation on 2026-11-09.

The approved destination will have plugin/ and site/ components and shared
contribution guidance. A separate combined adoption after import will establish
one root standards/factory configuration and one complete-toolchain factory
image for both components. Both toolchains, component READMEs and independent
component versions remain. Plugin releases use plugin-v… tags; site builds and
deployments do not require a plugin release. Co-location does not replace the
HTTP feed contract or require simultaneous deployments.

Only import PRs use ancestry-preserving merges; ordinary PRs, including combined
adoption and this decision record, retain squash delivery. Imported historical
site tags are namespaced against collisions, preserving original signed objects
and annotations without replaying publication. Published site releases are
retained. Open issues transfer and affected briefs are revalidated. The old site
repository is
archived with a destination pointer after verification. Existing plugin
releases and their historical tags remain accessible. Serving the static wall
and proving its public feed still belong to the hosting/live acceptance work;
source consolidation grants no deployment or host operation.

This records the chosen architecture; it does not claim that migration or live
acceptance has happened.

Accepted implementation contracts: [history import](https://github.com/lutzseverino/minecraft-stream-guard/issues/20),
[combined adoption](https://github.com/lutzseverino/minecraft-stream-guard/issues/21)
and [operational preparation](https://github.com/lutzseverino/homelab/issues/371).
