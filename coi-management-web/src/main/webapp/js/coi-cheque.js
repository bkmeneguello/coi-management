"use strict";

COI.module("Cheque", function(Module, COI, Backbone, Marionette, $, _) {

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
			resp.dataDeposito = $.datepicker.parseDate('yy-mm-dd', resp.dataDeposito);
			resp.cliente = new Pessoa(resp.cliente, {parse: true});
			resp.paciente = new Pessoa(resp.paciente, {parse: true});
			return resp;
		}
	});
	
	var ChequeView = COI.FormView.extend({
		template: '#cheque_template',
		regions: {
			'cliente': '#cliente',
			'paciente': '#paciente'
		},
		initialize: function() {
			if (!this.model.isNew()) {
				this.model.fetch({
					success: this.onFetch
				});
			}
		},
		onRender: function() {
			var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
			bindings['dataDeposito'].converter = dateConverter;
			bindings['valor'].converter = moneyConverter;
			this.modelBinder().bind(this.model, this.el, bindings);
			
			this.cliente.show(new COI.PessoaView({model: this.model.get('cliente'), label: 'Cliente:', attribute: 'cliente', required: true}));
			this.paciente.show(new COI.PessoaView({model: this.model.get('paciente'), label: 'Paciente:', attribute: 'paciente', required: true}));
		},
		onShow: function() {
			this.$el.find('input').first().focus();
		},
		onCancel: function(e) {
			Backbone.history.navigate('cheques', true);
		},
		onConfirm: function(e) {
			if (_validate(this)) {
				this.model.save(null, {wait: true, success: this.onComplete});
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