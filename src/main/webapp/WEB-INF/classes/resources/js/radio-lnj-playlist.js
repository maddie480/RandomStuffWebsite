{
    const artwork = [{
        src: "https://maddie480.ovh/img/lnj-512.png",
        sizes: "512x512",
        type: "image/png"
    }];

    // when playing a track, reset all other tracks
    const allAudioElements = document.getElementsByTagName('audio');

    for (let i = 0; i < allAudioElements.length; i++) {
        const audioElementBeingPlayed = allAudioElements[i];

        audioElementBeingPlayed.addEventListener('play', () => {
            for (let j = 0; j < allAudioElements.length; j++) {
                const otherAudioElement = allAudioElements[j];

                if (audioElementBeingPlayed !== otherAudioElement) {
                    otherAudioElement.pause();
                    otherAudioElement.currentTime = 0;
                }
            }

            if ('mediaSession' in navigator) {
                navigator.mediaSession.metadata = new MediaMetadata({
                    title: audioElementBeingPlayed.dataset.trackname,
                    artist: 'Radio LNJ \u2013 Playlist',
                    artwork
                });

                navigator.mediaSession.setActionHandler("play", () => {
                    audioElementBeingPlayed.play();
                });
            }
        });

        audioElementBeingPlayed.addEventListener('ended', () => {
            audioElementBeingPlayed.currentTime = 0;
        });
    }
}
