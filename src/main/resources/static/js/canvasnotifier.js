jQuery(document).ready(function($) {

    var buttons = $(':button');

    $(buttons).each(function() {
        $(this).click(function() {
            var action = $('#action');

            action.val($(this).val());
        });
    });

     // this will prevent forms from submitting twice
     $('form').preventDoubleSubmission();
});

// jQuery plugin to prevent double submission of forms
jQuery.fn.preventDoubleSubmission = function() {
    $(this).on('submit',function(e){
        var $form = $(this);

        if ($form.data('submitted') === true) {
            // Previously submitted - don't submit again
            e.preventDefault();
        } else {
            // Mark it so that the next submit can be ignored
            $form.data('submitted', true);

            $(".loading-inline").show();

            var buttons = $(':button');

            $(buttons).each(function() {
                $(this).addClass('disableSubmit');
                $(this).prop('disabled', true);
            });
        }
    });

    // Keep chainability
    return this;
}
