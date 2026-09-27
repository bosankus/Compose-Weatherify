#!/usr/bin/env bash
# Fetch a secret value from GCP Secret Manager (stdout only).
#
# Manual setup required (do NOT put secret values in git or use `gh secret set`
# for the Razorpay key — store it in GCP Secret Manager):
#   1. GCP Secret Manager secret with the API key value
#      (suggested secret id: weatherify-razorpay-key)
#   2. Workload Identity Federation + service account for GitHub OIDC
#   3. Repo Actions variables:
#        GCP_WORKLOAD_IDENTITY_PROVIDER
#        GCP_SERVICE_ACCOUNT
#        GCP_PROJECT_ID
#        GCP_SECRET_RAZORPAY   (secret id, e.g. weatherify-razorpay-key)
#
# Usage:
#   ./fetch-gcp-secret.sh --secret <SECRET_ID> [--project <PROJECT_ID>]
#   GCP_SECRET_ID=... GCP_PROJECT_ID=... ./fetch-gcp-secret.sh
#
# The secret value is written to stdout only. Callers capture it into an env
# var or file. Do not enable set -x around this script when capturing output.

set -euo pipefail

SECRET_ID="${GCP_SECRET_ID:-}"
PROJECT_ID="${GCP_PROJECT_ID:-}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --secret)
      SECRET_ID="${2:-}"
      shift 2
      ;;
    --project)
      PROJECT_ID="${2:-}"
      shift 2
      ;;
    -h|--help)
      sed -n '2,25p' "$0" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *)
      echo "error: unknown argument: $1" >&2
      echo "usage: $0 --secret <SECRET_ID> [--project <PROJECT_ID>]" >&2
      exit 1
      ;;
  esac
done

if [[ -z "${SECRET_ID}" ]]; then
  echo "error: secret id required (pass --secret or set GCP_SECRET_ID)" >&2
  exit 1
fi

if ! command -v gcloud >/dev/null 2>&1; then
  echo "error: gcloud CLI not found; install Google Cloud SDK or use google-github-actions/get-secretmanager-secrets" >&2
  exit 1
fi

# Access latest version; value goes to stdout only (never set -x on this line).
if [[ -n "${PROJECT_ID}" ]]; then
  gcloud secrets versions access latest --secret="${SECRET_ID}" --project="${PROJECT_ID}"
else
  gcloud secrets versions access latest --secret="${SECRET_ID}"
fi
