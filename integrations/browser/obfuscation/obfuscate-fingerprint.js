// Self-executing script injected before page scripts run (document_start)
(function() {
    'use strict';

    // 1. Canvas Fingerprint Obfuscation (Pseudo-random noise)
    const originalToDataURL = HTMLCanvasElement.prototype.toDataURL;
    const originalGetImageData = CanvasRenderingContext2D.prototype.getImageData;

    CanvasRenderingContext2D.prototype.getImageData = function(x, y, w, h) {
        const imageData = originalGetImageData.apply(this, arguments);
        // Inject micro-jitter (+/- 1 on alpha/color channel)
        for (let i = 0; i < imageData.data.length; i += 4) {
            imageData.data[i] = imageData.data[i] ^ (Math.floor(Math.random() * 2));
        }
        return imageData;
    };

    // 2. AudioContext Fingerprinting Protection
    if (window.AudioBuffer) {
        const originalGetChannelData = AudioBuffer.prototype.getChannelData;
        AudioBuffer.prototype.getChannelData = function() {
            const data = originalGetChannelData.apply(this, arguments);
            for (let i = 0; i < data.length; i += 100) {
                data[i] += (Math.random() - 0.5) * 0.0000001;
            }
            return data;
        };
    }

    // 3. Hardware & Concurrency Spoofing
    Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => 4 });
    Object.defineProperty(navigator, 'deviceMemory', { get: () => 8 });

    // 4. Strip WebRTC Local IP leaks
    if (window.RTCPeerConnection) {
        const origCreateOffer = RTCPeerConnection.prototype.createOffer;
        RTCPeerConnection.prototype.createOffer = function(options) {
            if (options && options.offerToReceiveAudio !== undefined) {
                options.offerToReceiveAudio = false;
            }
            return origCreateOffer.apply(this, arguments);
        };
    }
})();
