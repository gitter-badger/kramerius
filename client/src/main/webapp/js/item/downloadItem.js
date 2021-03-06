function DownloadItem() {}

DownloadItem.prototype.selectedMessage = function(act) {
    var act = act || this.selectAction();
    if (act != null) {
        if (act.object['message']) {
            return act.object.message;
        } else {
            return null;
        }
    }
}

DownloadItem.prototype.selectAction = function() {
    var v = $("#download_options select option:selected").val();
    var doptions = K5.gui.downloadoptions.ctx.actions;
    var reduced = _.reduce(doptions, function(memo, itm){ 
        if (memo  == null) {
            if (itm.name === v) return itm; 
            else return null;
        } else return memo;
    }, null);
    return reduced;
}

DownloadItem.prototype.doAction = function() {
    var act = this.selectAction();
    if (act != null) {
        act.object.doAction();
    }
    /*cleanWindow();*/
}

DownloadItem.prototype.init = function() {

    function _dialog(container, footer) {
        var downloadDiv = $("<div/>",{"id":"download","class":"download"});
        downloadDiv.css("position","absolute");
        downloadDiv.css("display","none");
        downloadDiv.append(container);
        downloadDiv.append(footer);
        return downloadDiv;
    }

    function _container(header, options, message) {
        var container = $("<div/>",{"id":"download_container"});
        container.append(header);
        container.append(options);
        if (message) {
            container.append(message);
        }
        return container;
    }
    
    function _message() {
        var optionsdiv = $("<div/>");
        var span = $("<span/>",{"id":"download_action_message"});
        optionsdiv.append(span);
        return optionsdiv;
    }
    
    function _options() {
        var optsCont = $("<div/>");
        var optionsdiv = $("<div/>",{"id":"download_options"});
        optsCont.append(optionsdiv);
        return optsCont;
    }
    
    function _footer(rdiv) {
        var footer = $("<div/>",{"class":"download_footer dialogs_footer"});
        footer.append(rdiv);
        return footer;
    }

    function _header() {
        function _div(v) { var retval = $("<div/>"); retval.append(v); return retval;  }

        var head = $("<div/>",{"class":"download_header"});
        var h2  = $("<h2/>");

        var sel = K5.api.ctx.item.selected;
        var title = K5.api.ctx.item[sel]["title"];
        var rootTitle = K5.api.ctx.item[sel]["root_title"];
        var model = K5.api.ctx.item[sel]["model"];
        
        h2.html(K5.i18n.translatable('downloads.title'));


        head.append(h2);
        return head;
    }

    function _rightbutton() {
        var rdiv = $("<div/>",{"class":"right"});
        var buttons = $("<div/>",{"class":"buttons"});
        rdiv.append(buttons);

        var okButton = $("<div/>",{"class":"button"});
        okButton.attr('onclick',"K5.gui.selected.download.doAction();");
        okButton.attr("data-ctx","selection;pdflimit");

        okButton.append(K5.i18n.translatable('common.ok'));

        var closeButton = $("<div/>",{"class":"button"});
        closeButton.attr('onclick',"cleanWindow();");
        closeButton.attr("data-ctx","selection;pdflimit");

        closeButton.append(K5.i18n.translatable('common.close'));

        buttons.append(okButton);
        buttons.append(closeButton);

        return rdiv;
    }

    $('#viewer>div.container').append(_dialog(_container(_header(), _options(),_message()), _footer(_rightbutton())));
}


DownloadItem.prototype.open = function() {
    cleanWindow();
    divopen("#download");
    var doptions = K5.gui.downloadoptions.ctx.actions;
    var select = $('<select/>');
    select.change(function() {
        var message = K5.gui.selected.download.selectedMessage();
        if (message != null) {
            $("#download_action_message").text(message);
        } else {
            $("#download_action_message").text("");
        }
    });
    var options = _.map(doptions, function(a) {
        if (a.object.enabled()) {
            var optHtml =$('<option/>', {'value': a.name,'data-key': a.i18nkey});
            optHtml.html(K5.i18n.translatable(a.i18nkey));
            var option = {
                    "elem":optHtml
            };
            if (a.object["message"]) {
                option["message"] = a.object["message"];
            }
            return option;
        } else return null;
    });

    _.each(options, function(opt) {
        if (opt != null) {
            select.append(opt.elem);
        }
    });

    var first = _.reduce(options, function(memo, value, index) {
        if (memo == null) {
            memo = value;
        }
        return memo;
    }, null);

    var message = first["message"];
    if ((message) && (message != null)) {
        $("#download_action_message").text(message);
    } else {
        $("#download_action_message").text("");
    }
    $("#download_options").html(select);
}

DownloadItem.prototype.cleanDialog = function() {
    $("#download_options").empty();
}