# JobLink notification boundary

The Android app only registers an FCM app instance, stores its Firebase Installation ID under the authenticated user's document, receives messages, displays notifications, and routes notification taps. It does not decide when another user should receive a notification and it does not contain Firebase Admin credentials.

Registration documents use this path:

`users/{uid}/messagingRegistrations/{hashedInstallationId}`

Each document contains `installationId`, `platform`, and a server-generated `updatedAt` timestamp. The hash is only the Firestore document key; the trusted sender uses the stored installation ID.

## Data payload contract

Send one supported `event` value and the related identifier:

| Event | Required routing data |
| --- | --- |
| `APPLICATION_REVIEWED` | `applicationId` |
| `SHORTLISTED` | `applicationId` |
| `INTERVIEW` | `applicationId` |
| `JOB_OFFER` | `applicationId` |
| `NEW_MATCHING_JOB` | `jobId` |

Optional `title` and `body` data values override the app's local fallback text. Use string values for every FCM data field. Missing routing identifiers safely open the Job Seeker home screen.

## Trusted backend required

Automatic delivery requires Cloud Functions or another trusted backend using the Firebase Admin SDK. That backend should:

- react to an employer-authorized application status change and notify only the associated applicant;
- determine recipients for a newly active matching job and send `NEW_MATCHING_JOB` with its `jobId`;
- read a recipient's current messaging registrations with Admin SDK privileges;
- validate that the event belongs to that recipient before sending;
- remove registrations that FCM reports as invalid or unregistered.

Service-account keys and other privileged credentials belong only in the deployed backend's managed environment. They must never be copied into `google-services.json`, Android resources, Gradle files, or Kotlin code.
