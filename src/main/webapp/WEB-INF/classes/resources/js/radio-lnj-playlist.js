{
    // when playing a track, reset all other tracks
    const allAudioElements = document.getElementsByTagName('audio');

    for (let i = 0; i < allAudioElements.length; i++) {
        const audioElementBeingPlayed = allAudioElements[i];

        audioElementBeingPlayed.addEventListener('play', () => {
            for (let j = 0; j < allAudioElements.length; j++) {
                const otherAudioElement = allAudioElements[j];

                if (audioElementBeingPlayed !== otherAudioElement && !otherAudioElement.paused) {
                    otherAudioElement.pause();
                    otherAudioElement.currentTime = 0;
                }
            }
        });
    }
}
