# AWS setup

Do this **the moment the account activates**, before creating a single resource.
Total time: about 15 minutes.

---

## 1. Budget alarm — do this first

Console → search **"Billing and Cost Management"** → **Budgets** (left sidebar) → **Create budget**.

| Field | Value |
|---|---|
| Budget setup | **Customize (advanced)** |
| Budget type | **Cost budget** |
| Name | `monthly-ceiling` |
| Period | **Monthly** |
| Budget renewal type | **Recurring** |
| Budgeting method | **Fixed** |
| Budgeted amount | **20** |

Then **Configure alerts** → **Add alert threshold**, and add three:

| Threshold | Type | Sends at |
|---|---|---|
| 50% of budgeted amount | Actual | $10 spent |
| 80% of budgeted amount | Actual | $16 spent |
| 100% of budgeted amount | **Forecasted** | on track to exceed $20 |

Email recipient: `karasmiti93@gmail.com` on all three.

The forecasted one matters most — it warns you while there is still time to act, rather than
after the money is gone.

> Credits do not suppress these alerts. Spend is still metered and reported, the credits just pay
> the bill. That is what you want: the alarm tells you about a mistake while the credits are
> absorbing it.

## 2. Free-tier usage alerts

Billing and Cost Management → **Billing preferences** → tick:

- **Receive AWS Free Tier alerts**
- **Receive CloudWatch billing alerts**

## 3. Stop using the root account

The account you signed up with is the root user. It can delete everything and cannot be restricted.
Use it once for the steps above, then stop.

1. **Root MFA:** click your account name (top right) → **Security credentials** → **Multi-factor
   authentication (MFA)** → assign an authenticator app.
2. **Create your daily user:** IAM → **Users** → **Create user** → name `dmytro`.
   - Tick *Provide user access to the AWS Management Console*.
   - Attach policy **AdministratorAccess** directly.
   - After creation, open the user → **Security credentials** → assign MFA.
3. Sign out of root. Sign back in as `dmytro` using the account-ID sign-in URL that IAM shows you.
4. Store the root credentials somewhere you will not use them casually.

## 4. Credentials for Terraform — later, chapter 33

Do **not** create access keys yet. When chapter 33 needs them:

- IAM → Users → `dmytro` → Security credentials → **Create access key** → *Command Line Interface*.
- `aws configure --profile pitwall`, never as environment variables in the repo.
- Never a root access key. Never a key in git.

## 5. Cost traps to avoid in chapter 33

None of these are needed by this project, and each one quietly eats the credits.

| Trap | Cost | Instead |
|---|---|---|
| **NAT Gateway** | ~$32/month, idle or not | Public subnet + security groups |
| Oversized RDS | $30–80/month | `db.t4g.micro` |
| Unattached Elastic IP | ~$3.60/month each | Release when done |
| Stopped instance with volumes | volumes still bill | Terminate, do not stop |
| Multi-AZ RDS | doubles the bill | Single AZ; this is not production |

## 6. When credits run low

The free plan can **suspend resources** rather than bill you when credits run out. That would take
the live demo down, possibly while someone is looking at it.

Before the week 4 deploy, either switch to the paid plan — unspent credits carry over — or make sure
the 80% alert gives you enough warning to act.

---

## Domain

Register it **separately, now** — do not wait for AWS and do not use Route 53 to register.
Porkbun, Namecheap or Cloudflare are cheaper and resolve within the hour. Point DNS at the server
in chapter 14.
