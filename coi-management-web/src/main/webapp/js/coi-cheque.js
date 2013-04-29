"use strict";

COI.module("Entrada", function(Module, COI, Backbone, Marionette, $, _) {

	var Pessoa = Backbone.Model.extend({
		defaults: {
			nome: null,
			codigo: null
		}
	});
	
	var Cheque = Backbone.Model.extend({
		urlRoot: '/rest/cheques',
		defaults: function() {
			return {
				numero: null,
				conta: null,
				agencia: null,
				banco: null,
				documento: null,
				valor: null,
				dataDeposito: null,
				observacao: null,
				cliente: new Pessoa(),
				paciente: new Pessoa()
			};
		},
		parse: function(resp, options) {
			resp.dataDeposito = new Date(resp.dataDeposito);
			resp.cliente = new Pessoa(resp.cliente);
			resp.paciente = new Pessoa(resp.paciente);
			return resp;
		}
	});
	
	var ChequeView = Marionette.ItemView.extend({
		template: '#cheque_template',
		tagName: 'form',
		cliente: new Pessoa(),
		paciente: new Pessoa(),
		modelBinder: function() {
			return new Backbone.ModelBinder();
		},
		events: {
			'click .coi-action-confirm': 'doConfirm',
			'click .coi-action-cancel': 'doCancel'
		},
		ui: {
			'cliente': '#cliente',
			'paciente': '#paciente'
		},
		initialize: function() {
			_.bindAll(this);
			if (this.model.isNew()) {
				this.onNew();				
			} else {
				this.model.fetch({
					success: this.onFetch
				});
			}
		},
		onRender: function() {
			var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
			bindings['dataDeposito'].converter = dateConverter;
			bindings['valor'].converter = autoNumericConverter;
			this.modelBinder().bind(this.model, this.el, bindings);
			this.$el.form();
			
			var that = this;
			this.ui.cliente.autocomplete({
				source: '/rest/pessoas',
				minLength: 3,
				appendTo: this.ui.cliente.closest('.coi-form-item'),
				response: function(event, ui) {
					$.each(ui.content, function(index, element) {
						element.label = '[' + element.codigo + '] ' + element.nome;
						element.value = element.nome;
					});
				},
				select: function(event, ui) {
					var element = ui.item;
					that.model.set('cliente', new Pessoa(_.omit(element, 'label', 'value', 'partes')));
				}
			})
			.change(function() {
				if ($(this).val() != that.model.get('cliente').get('nome')) {
					$(this).val(null);
				}
			})
			.blur(function() {
				if ($.isBlank($(this).val())) {
					that.model.get('cliente').clear();
				} else if (that.model.get('cliente').isNew()) {
					$(this).val(null).blur();
				}
			})
			.autocomplete('widget')
			.css('z-index', 100);

			this.ui.paciente.autocomplete({
				source: '/rest/pessoas',
				minLength: 3,
				appendTo: this.ui.paciente.closest('.coi-form-item'),
				response: function(event, ui) {
					$.each(ui.content, function(index, element) {
						element.label = '[' + element.codigo + '] ' + element.nome;
						element.value = element.nome;
					});
				},
				select: function(event, ui) {
					var element = ui.item;
					that.model.set('paciente', new Pessoa(_.omit(element, 'label', 'value', 'partes')));
				}
			})
			.change(function() {
				if ($(this).val() != that.model.get('paciente').get('nome')) {
					$(this).val(null);
				}
			})
			.blur(function() {
				if ($.isBlank($(this).val())) {
					that.model.get('paciente').clear();
				} else if (that.model.get('paciente').isNew()) {
					$(this).val(null).blur();
				}
			})
			.autocomplete('widget')
			.css('z-index', 100);
		},
		onShow: function() {
			this.$el.find('input').first().focus();
		},
		onNew: function() {
			
		},
		onFetch: function() {
			this.ui.cliente.val(this.model.get('cliente').get('nome'));
			this.ui.paciente.val(this.model.get('paciente').get('nome'));
			this.$el.form();
		},
		doCancel: function(e) {
			e.preventDefault();
			Backbone.history.navigate('entradas', true);
		},
		doConfirm: function(e) {
			e.preventDefault();
			var that = this;
			if (_validate(this)) {
				this.model.save(null, {wait: true, success: that.onComplete});
			}
		},
		onComplete: function() {
			Backbone.history.navigate('cheques', true);
			_notifySuccess();
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('cheque', function() {
			COI.body.show(new ChequeView({model: new Cheque()}));
		});
		COI.router.route('cheque(/:id)', function(id) {
			COI.body.show(new ChequeView({model: new Cheque({id: id})}));
		});
	});
});