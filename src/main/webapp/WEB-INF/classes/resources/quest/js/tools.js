$(".tool-image").on("click", function(event) {
    $("#tool-image-modal-title").html("Image - " + $(event.target).attr("data-name"));
    $("#tool-image-modal").attr("src", $(event.target).attr("src"));
});