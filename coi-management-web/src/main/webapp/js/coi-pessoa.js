"use strict";

COI.module("Pessoa", function(Module, COI, Backbone, Marionette, $, _) {
	
	var Pessoa = Backbone.Model.extend({
		urlRoot: 'rest/pessoas',
		defaults: function() {
			return {
				nome: null,
				codigo: null
			};
		}
	});
	
	var PessoaView = COI.FormView.extend({
		template: '#pessoa_template',
		triggers: {
			'keyup input[name=codigo]': 'changeCodigo'
		},
		initialize: function() {
			if (!this.model.isNew()) {
				this.model.fetch();
			}
		},
		onRender: function() {
			this.modelBinder().bind(this.model, this.el);
		},
		onShow: function() {
			this.$el.find('input').first().focus();
		},
		onChangeCodigo: function(e) {
			var input = this.$("input[name=codigo]");
			var codigo = input.val().toUpperCase();
			if (codigo.length == 1) {
				$.get('rest/pessoas/next', {'prefix': codigo}, function(max) {
					input.val(codigo + "-" + max);
					input.get(0).setSelectionRange(2, input.val().length);
					input.change();
				});
			}
		},
		onCancel: function(e) {
			Backbone.history.navigate('pessoas', true);
		},
		onConfirm: function(e) {
			if (_validate(this)) {
				this.model.save(null, {wait: true, success: this.onComplete, error: this.onError});
			}
		},
		onComplete: function() {
			Backbone.history.navigate('pessoas', true);
			_notifySuccess();
		},
		onError: function(model, resp, options) {
			_notifyUpdateFailure();
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('pessoa', function() {
			COI.body.show(new PessoaView({model: new Pessoa()}));
		});
		COI.router.route('pessoa(/:id)', function(id) {
			COI.body.show(new PessoaView({model: new Pessoa({id: id})}));
		});
	});
});