"use strict";

function _validate(element) {
	element.$(':coi-validate').validate('validate');
	if(element.$('.coi-validation-invalid').length) {
		_notifyValidation();
		return false;
	}
	return true;
}

function _notifyValidation() {
	noty({text: 'Verifique todos os campos', type: 'warning', timeout: 5000});
}

function _notifySuccess() {
	noty({text: 'Registro incluido com sucesso', type: 'success', timeout: 2000});
}

function _notifyDelete() {
	noty({text: 'Registro excluido com sucesso', type: 'success', timeout: 2000});
}

function _notifyDeleteFailure() {
	noty({text: 'Falha na exclusão do registro', type: 'error'});
}

function _promptDelete(confirm) {
	noty({
		text: 'Deseja realmente excluir o registro?',
		layout: 'center',
		buttons: [
		    {
			    addClass: 'btn',
			    text: 'Cancelar',
			    onClick: function($noty) {
			        $noty.close();
		    	}
		 	},
			{
				addClass: 'btn', 
				text: 'Confirmar', 
				onClick: function($noty) {
					confirm();
					$noty.close();					
				}
		    }
		],
		callback: {
			onShow: function() {
				$('button.btn').button();
			}
		}
	});
}

function autoNumericConverter(direction, value, attribute, model) {
	switch(direction) {
	case 'ModelToView':
		return model.get(attribute);
		break;
	case 'ViewToModel':
		return $(this.boundEls).autoNumeric('get');
		break;
	}
};

var COI = new Backbone.Marionette.Application();
COI.addRegions({
	'body': 'body'
});

COI.addInitializer(function (options) {
	this.router = new Backbone.Router();
});

COI.on('start', function(options) {
	Backbone.history.start();
});

COI.module('Index', function(Module, COI, Backbone, Marionette, $, _) {
	var IndexView = Marionette.ItemView.extend({
		events: {
			'click #categorias': 'categorias',
			'click #pessoas': 'pessoas',
			'click #entradas': 'entradas',
			'click #cheques': 'cheques'
		},
		template: '#index_template',
		initialize: function() {
			_.bindAll(this);
		},
		onRender: function() {
			this.$('button').button();
		},
		categorias: function(e) {
			Backbone.history.navigate('categorias', true);
		},
		pessoas: function(e) {
			Backbone.history.navigate('pessoas', true);
		},
		entradas: function(e) {
			Backbone.history.navigate('entradas', true);
		},
		cheques: function(e) {
			Backbone.history.navigate('cheques', true);
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('', function() {
			COI.body.show(new IndexView());
		});
	});
});