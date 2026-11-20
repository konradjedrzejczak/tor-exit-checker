#!/usr/bin/env bash
# Removes everything created for the AWS deployment so nothing keeps costing money.
# Usage: AWS_REGION=eu-north-1 ./deploy/cleanup.sh [stack-name]
set -euo pipefail

STACK="${1:-tor-exit-checker}"
REGION="${AWS_REGION:?set AWS_REGION, e.g. eu-north-1}"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
BUCKET="tor-exit-checker-artifacts-${ACCOUNT_ID}-${REGION}"

echo "1/4 Deleting the ECS service (created by the workflow, so CloudFormation does not know it)"
if [ -n "$(aws ecs describe-services --region "$REGION" --cluster tor-exit-checker --services tor-exit-checker \
      --query "services[?status=='ACTIVE'].serviceName" --output text 2>/dev/null)" ]; then
  aws ecs delete-service --region "$REGION" --cluster tor-exit-checker --service tor-exit-checker --force >/dev/null
  aws ecs wait services-inactive --region "$REGION" --cluster tor-exit-checker --services tor-exit-checker
fi

echo "2/4 Emptying the S3 bucket (CloudFormation cannot delete a bucket that has objects)"
aws s3 rm "s3://${BUCKET}" --recursive --region "$REGION" || true

echo "3/4 Deleting the CloudFormation stack"
aws cloudformation delete-stack --region "$REGION" --stack-name "$STACK"
aws cloudformation wait stack-delete-complete --region "$REGION" --stack-name "$STACK"

echo "4/4 Done. Check the EC2, ECS, ECR, S3 and VPC consoles once to confirm nothing is left."
