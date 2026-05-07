# Security Notes

## Runtime Configuration

Runtime env files under `config/.…` and root `.env` are local-only and must not be committed.
Store real values in local files or deployment secrets, not in Git.
For Plaid adapter tunneling, set `TUNA_TOKEN` in `config/.plaid-adapter-service`.

## Previously Committed Secrets

Removing a secret from the current tree does not remove it from Git history or existing GitHub clones.
Any value that was committed should be treated as exposed and rotated in the provider/system that issued it.

To purge old values from GitHub history, rewrite repository history with a tool such as `git filter-repo` or BFG and force-push all affected branches. Do this only after coordinating with every collaborator because all existing clones must be re-synchronized.
