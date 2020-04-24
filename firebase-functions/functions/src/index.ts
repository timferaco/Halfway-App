import * as functions from 'firebase-functions';
import { LatLng, DirectionsResponse, DirectionsReponseStatus, GeocodedWaypoint } from '@google/maps';
// // Start writing Firebase Functions
// // https://firebase.google.com/docs/functions/typescript
//

const googleMapsClient = require('@google/maps').createClient({
    key: 'your API key here'
  });

export const helloWorld = functions.https.onRequest((request, response) => {
    console.log("Hello World! Testing Firebase cloud functions!")
    response.send("Hello from Firebase!");
});

function findMidpoint(origin:LatLng, dest:LatLng){
    var directionsService = new googleMapsClient.directionsService();
    var waypoints;
    
    directionsService.route(
      {
        origin: origin,
        destination: dest,
        travelMode: 'DRIVING'
      },
      function(response:DirectionsResponse, status:DirectionsReponseStatus) {
        if (status === 'OK') {
          waypoints = response.geocoded_waypoints;
        } else {
          window.alert('Directions request failed due to ' + status);
        }
      });

      var midpoint = calcMidpoint(waypoints);
}

function calcMidpoint(waypoints:Array<GeocodedWaypoint>){
  var originList = new Array<GeocodedWaypoint>();
  var destList = new Array<GeocodedWaypoint>();

  //initially add two points to both the origin and des to establish baseline
  originList.push(waypoints[0]);
  originList.push(waypoints[1]);
  waypoints.splice(0,2);

  destList.push(waypoints[waypoints.length-1]);
  destList.push(waypoints[waypoints.length-2]);
  waypoints.pop();
  waypoints.pop();

  //calculate starting distance
  var startDist = googleMapsClient.geometry.spherical.computeDistanceBetween(new LatLng(originList[0].))

  
}