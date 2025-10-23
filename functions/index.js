const functions = require("firebase-functions");
const { onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2");
const admin = require("firebase-admin");
const { getMessaging } = require("firebase-admin/messaging");

admin.initializeApp();
setGlobalOptions({ region: "europe-west6" });

exports.proximityNotifier = onDocumentUpdated("users/{userId}", async (event) => {
  const beforeData = event.data.before.data();
  const afterData = event.data.after.data();

  if (!afterData || !beforeData?.location || !afterData?.location || beforeData.location.isEqual(afterData.location)) {
    return;
  }

  const userLocation = afterData.location;
  const userFcmToken = afterData.fcmToken;
  const userId = event.params.userId;

  if (!userFcmToken) {
    functions.logger.warn(`Korisnik ${userId} nema FCM token.`);
    return;
  }

  functions.logger.info(`Proveravam blizinu za korisnika ${userId}...`);

  const spotsSnapshot = await admin.firestore().collection("recycling_spots").get();

  const notifiedSpots = afterData.notifiedSpots || {};
  const now = Date.now();
  const NOTIFICATION_COOLDOWN = 60 * 60 * 1000; // 1 sat

  const spotsToSendNotificationFor = [];
  const newTimestamps = {};

  spotsSnapshot.forEach((doc) => {
    const spot = doc.data();
    const spotId = doc.id;

    if (spot.location && spot.name) {
      const distance = calculateDistance(
        userLocation.latitude, userLocation.longitude,
        spot.location.latitude, spot.location.longitude
      );

      if (distance < 0.5) { // 500 metara
        const lastNotified = notifiedSpots[spotId];
        if (!lastNotified || (now - lastNotified > NOTIFICATION_COOLDOWN)) {
          spotsToSendNotificationFor.push(spot.name);
          newTimestamps[spotId] = now;
        }
      }
    }
  });

  if (spotsToSendNotificationFor.length > 0) {
    const spotNames = spotsToSendNotificationFor.join(", ");
    functions.logger.info(`Šaljem notifikaciju za nove tačke: ${spotNames}`);

    const payload = {
      notification: {
        title: "Reciklažna tačka u blizini!",
        body: `Nalazite se blizu: ${spotNames}`,
      },
      token: userFcmToken,
    };

    try {
      await getMessaging().send(payload);
      functions.logger.info("Notifikacija uspešno poslata.");


      const dataToMerge = {
        notifiedSpots: {
          ...notifiedSpots,
          ...newTimestamps
        }
      };
      await admin.firestore().collection("users").doc(userId).set(dataToMerge, { merge: true });
      functions.logger.info("Timestamp-ovi za notifikacije su ažurirani.");

    } catch (error) {
      functions.logger.error("Greška pri slanju ili ažuriranju timestamp-a:", error);
    }
  } else {
    functions.logger.info("Nema novih obližnjih tačaka za notifikaciju.");
  }
});


function calculateDistance(lat1, lon1, lat2, lon2) {
    const R = 6371;
    const dLat = (lat2 - lat1) * (Math.PI / 180);
    const dLon = (lon2 - lon1) * (Math.PI / 180);
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(lat1 * (Math.PI / 180)) * Math.cos(lat2 * (Math.PI / 180)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}