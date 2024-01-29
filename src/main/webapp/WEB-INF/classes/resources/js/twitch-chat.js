{
    // thanks to https://stackoverflow.com/a/8076436
    const hashCode = function (string) {
        let hash = 0;
        for (let i = 0; i < string.length; i++) {
            let code = string.charCodeAt(i);
            hash = ((hash << 5) - hash) + code;
            hash = hash & hash; // Convert to 32bit integer
        }
        return hash;
    }

    const makeMessage = function () {
        const messageDOM = document.createElement('div');
        messageDOM.className = 'standby';
        document.getElementById("body").appendChild(messageDOM);
        return messageDOM;
    };

    let currentMessageDOM = makeMessage();

    const addElement = function (into, text, className) {
        const element = document.createElement('span');
        element.innerText = text;
        element.className = className;
        into.appendChild(element);
    }

    const expireMessage = function (element) {
        setTimeout(function () {
            element.className = 'leadout';
            setTimeout(function () {
                document.getElementById('body').removeChild(element);
            }, 2000);
        }, 30000);
    }

    const runSocket = function () {
        const socket = new WebSocket('ws://live-chat.maddie480.ovh:11586/');

        socket.onopen = function (e) {
            console.log('Socket connected!');
        }

        socket.onmessage = function (e) {
            const message = JSON.parse(e.data);
            console.log('Message received', message);

            if (message.platform === 'twitch') {
                message.badges.splice(0, 0, 'https://maddie480.ovh/img/twitch.png');
            }
            if (message.platform === 'youtube') {
                message.badges.splice(0, 0, 'https://maddie480.ovh/img/youtube.ico');
            }

            const author = document.createElement('span');
            author.className = 'author';
            for (let i = 0; i < message.badges.length; i++) {
                const image = document.createElement('img');
                image.src = message.badges[i];
                author.appendChild(image);
            }
            addElement(author, message.author, 'color-' + (Math.abs(hashCode(message.author)) % 15));
            currentMessageDOM.appendChild(author);

            const messageContainer = document.createElement('div');
            addElement(messageContainer, message.message, 'message');
            currentMessageDOM.appendChild(messageContainer);

            currentMessageDOM.className = 'shown';
            expireMessage(currentMessageDOM);
            currentMessageDOM = makeMessage();
            socket.send(message.ack);
        }

        socket.onclose = function(event) {
            console.log('Connection closed, wasClean =', event.wasClean);
            setTimeout(runSocket, 5000);
        };
    };

    runSocket();
}
