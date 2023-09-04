{
    const audio = new Audio();
    let playlist = [];
    let updateTimerTimeoutHandle;
    let nextSongTimeoutHandle;

    const getTrackName = () => {
        const length = (playlist[0].duration - 5000 ) / 1000;
        let minutes = Math.floor(length / 60);
        let seconds = Math.floor(length % 60);

        if (minutes < 10) minutes = '0' + minutes;
        if (seconds < 10) seconds = '0' + seconds;

        return playlist[0].trackName + ' (' + minutes + ':' + seconds + ')';
    }

    const updateTimer = () => {
        if (audio.paused) {
            document.getElementById('timer').innerText = '-- --';
            updateTimerTimeoutHandle = setTimeout(updateTimer, 1000);
        } else {
            const progress = audio.currentTime;
            let minutes = Math.floor(progress / 60);
            let seconds = Math.floor(progress % 60);

            // eh, might as well abuse the "JS is not typed" thingy
            if (minutes < 10) minutes = '0' + minutes;
            if (seconds < 10) seconds = '0' + seconds;

            document.getElementById('timer').innerText = minutes + ' ' + seconds;

            const millisUntilNextSecond = 1000 - ((progress - Math.floor(progress)) * 1000);
            updateTimerTimeoutHandle = setTimeout(updateTimer, millisUntilNextSecond);
        }
    };

    const nextSong = () => {
        const doneElement = playlist.splice(0, 1);
        playlist.push(doneElement);

        audio.src = playlist[0].path;
        audio.currentTime = 0;
        document.getElementById('title').innerText = getTrackName();
        audio.play();
        nextSongTimeoutHandle = setTimeout(nextSong, playlist[0].duration);
    };

    const startPlaying = async function() {
        const playlistResource = await (await fetch('/radio-lnj/playlist.json')).json();
        playlist = playlistResource.playlist;

        audio.src = playlist[0].path;
        audio.currentTime = playlistResource.seek / 1000.;
        document.getElementById('title').innerText = getTrackName();
        audio.play();

        nextSongTimeoutHandle = setTimeout(nextSong, playlist[0].duration - playlistResource.seek);
        updateTimer();

        document.getElementById('play').removeEventListener('click', startPlaying);
        document.getElementById('play').addEventListener('click', stopPlaying);
        document.getElementById('play').innerText = 'Stop';
    };

    const stopPlaying = () => {
        audio.pause();

        clearTimeout(updateTimerTimeoutHandle);
        clearTimeout(nextSongTimeoutHandle);

        document.getElementById('timer').innerText = '-- --';
        document.getElementById('title').innerText = '--';

        document.getElementById('play').removeEventListener('click', stopPlaying);
        document.getElementById('play').addEventListener('click', startPlaying);
        document.getElementById('play').innerText = 'Play';
    };

    document.getElementById('play').addEventListener('click', startPlaying);
}
