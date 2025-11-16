# Firebase Cloud Messaging Setup

1. **Enable FCM** in the Firebase console for the existing project and download the updated `google-services.json` if prompted.
2. **Obtain server credentials** from *Project Settings → Service Accounts* and generate a private key for the Firebase Admin SDK. Keep this JSON key safe and do **not** commit it to version control.
3. **Deploy a Cloud Function** (or any backend) that sends reminder payloads. Example (Node.js / TypeScript):

```ts
import * as admin from "firebase-admin";

admin.initializeApp();

export async function sendReminderToUser(userId: string, title: string, body: string) {
  const tokensSnap = await admin.database().ref(`users/${userId}/fcmTokens`).get();
  if (!tokensSnap.exists()) return;
  const tokens = Object.keys(tokensSnap.val() as Record<string, true>);
  if (!tokens.length) return;

  const payload: admin.messaging.MulticastMessage = {
    tokens,
    data: {
      title,
      body,
    },
    notification: {
      title,
      body,
    },
  };

  await admin.messaging().sendEachForMulticast(payload);
}
```

Schedule the function with Cloud Scheduler or trigger it when a deadline is reached. The client app now keeps user tokens synced and will surface any data/notification payload delivered via FCM. 
