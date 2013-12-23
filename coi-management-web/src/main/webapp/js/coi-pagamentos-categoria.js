"use strict";

COI.module("PagamentosCategoria", function(Module, COI, Backbone, Marionette, $, _) {
	
	var Categoria = Backbone.Model.extend({
		urlRoot: '/rest/pagamentos/categorias',
		defaults: {
			descricao: null
		}
	});
	
	var View = COI.FormView.extend({
		template: '#pagamento_categoria_template',
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
		onCancel: function(e) {
			Backbone.history.navigate('pagamento-categorias', true);
		},
		onConfirm: function(e) {
			if (_validate(this)) {
				this.model.save(null, {wait: true, success: this.onComplete, error: this.onError});
			}
		},
		onComplete: function(e) {
			Backbone.history.navigate('pagamento-categorias', true);
			_notifySuccess();
		},
		onError: function(model, resp, options) {
			_notifyError(resp.responseText);
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('pagamento-categoria', function() {
			COI.body.show(new View({model: new Categoria()}));
		});
		COI.router.route('pagamento-categoria(/:id)', function(id) {
			COI.body.show(new View({model: new Categoria({id: id})}));
		});
	});
});