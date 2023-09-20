{
    const uuid = document.getElementById('table').dataset.pollUuid;

    // fetch all the HTML <td> elements that match the poll choices
    const pollChoiceElements = {};
    const allElements = document.getElementsByTagName('td');
    for (let i = 0; i < allElements.length; i++) {
        const element = allElements[i];
        pollChoiceElements[element.dataset.choice] = element;
    }

    const refresh = async function () {
        setTimeout(refresh, 5000);
        const results = await (await fetch('/twitch-polls/' + uuid + ".json")).json();

        const allAnswers = Object.entries(results.answers);
        const totalAnswerCount = Object.values(results.answers).reduce((a, b) => a + b);

        for (let i = 0; i < allAnswers.length; i++) {
            const answerName = allAnswers[i][0];
            const answerCount = allAnswers[i][1];
            const percent = Math.round(100.0 * answerCount / totalAnswerCount);

            pollChoiceElements[answerName].querySelector('.quantity').innerText = percent + '% (' + answerCount + (answerCount === 1 ? ' vote' : ' votes') + ')';
            pollChoiceElements[answerName].querySelector('.bar').style.width = percent + '%';
        }
    }

    refresh();
}
